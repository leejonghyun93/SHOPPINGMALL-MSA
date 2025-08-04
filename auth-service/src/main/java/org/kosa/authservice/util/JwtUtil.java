package org.kosa.authservice.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Function;

@Component
@Slf4j
public class JwtUtil {

    @Value("${jwt.secret:rrYd2zPDUkx7BUhgDsOTxHCbsBkeTgE/uoARWYSqBjU=}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400000}") // 24시간 (밀리초)
    private Long jwtExpiration;

    private SecretKey getSigningKey() {
        // JWT secret이 32바이트 이상인지 확인
        if (jwtSecret.length() < 32) {
            log.warn("JWT secret key가 너무 짧습니다. 최소 32바이트 필요");
            jwtSecret = "rrYd2zPDUkx7BUhgDsOTxHCbsBkeTgE/uoARWYSqBjU="; // 기본값으로 확장
        }
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 보안 강화된 JWT 토큰 생성 - 이름 포함 가능
     */
    public String generateToken(String userId) {
        return generateToken(userId, "USER", null);
    }

    public String generateToken(String userId, String role) {
        return generateToken(userId, role, null);
    }

    /**
     *  이름을 포함한 JWT 토큰 생성 (소셜 로그인용)
     */
    public String generateToken(String userId, String role, String name) {
        Date now = new Date(System.currentTimeMillis());
        Date expiryDate = new Date(now.getTime() + jwtExpiration);

        log.info("JWT 토큰 생성 - userId: '{}', role: '{}', name: '{}'", userId, role, name);

        try {
            JwtBuilder builder = Jwts.builder()
                    .setSubject(userId) // 사용자 식별자를 subject로 설정
                    .claim("username", userId) // username과 userId를 동일하게 처리
                    .claim("role", role != null ? role : "USER")
                    .claim("type", "access") // 토큰 타입
                    .setIssuedAt(now)
                    .setExpiration(expiryDate)
                    .setIssuer("auth-service"); // 발급자

            // 이름 처리 로직 개선
            if (name != null && !name.trim().isEmpty()) {
                String cleanName = name.trim();

                // 기본값들 제외 조건 완화
                if (!cleanName.equals("사용자") &&
                        !cleanName.equals("소셜사용자") &&
                        !cleanName.equals(userId) &&
                        cleanName.length() > 0) {

                    builder.claim("name", cleanName);
                    log.info("토큰에 이름 포함: '{}'", cleanName);
                } else {
                    log.info("토큰에 이름 미포함 - 기본값 처리: '{}'", cleanName);
                }
            } else {
                log.info("토큰에 이름 미포함 - name이 null 또는 빈 문자열: '{}'", name);
            }

            String token = builder.signWith(getSigningKey(), SignatureAlgorithm.HS256).compact();

            // 생성된 토큰의 내용 확인 (디버깅용)
            try {
                Claims claims = getAllClaimsFromToken(token);
                log.info(" 생성된 토큰 내용 확인:");
                log.info("  - subject: {}", claims.getSubject());
                log.info("  - username: {}", claims.get("username"));
                log.info("  - name: {}", claims.get("name"));
                log.info("  - role: {}", claims.get("role"));
            } catch (Exception e) {
                log.warn("토큰 내용 확인 실패: {}", e.getMessage());
            }

            return token;
        } catch (Exception e) {
            log.error("JWT 토큰 생성 실패: {}", e.getMessage(), e);
            throw new RuntimeException("JWT 토큰 생성에 실패했습니다", e);
        }
    }

    /**
     * 리프레시 토큰 생성 (더 긴 만료시간, 최소 정보)
     */
    public String generateRefreshToken(String userId) {
        Date now = new Date(System.currentTimeMillis());
        Date expiryDate = new Date(now.getTime() + (jwtExpiration * 7)); // 7일

        try {
            return Jwts.builder()
                    .setSubject(userId)
                    .claim("type", "refresh")
                    .setIssuedAt(now)
                    .setExpiration(expiryDate)
                    .setIssuer("auth-service")
                    .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                    .compact();
        } catch (Exception e) {
            log.error("리프레시 토큰 생성 실패: {}", e.getMessage(), e);
            throw new RuntimeException("리프레시 토큰 생성에 실패했습니다", e);
        }
    }

    /**
     * 토큰에서 사용자 ID 추출
     */
    public String getUserIdFromToken(String token) {
        try {
            return getClaimFromToken(token, Claims::getSubject);
        } catch (Exception e) {
            log.error("토큰에서 사용자 ID 추출 실패: {}", e.getMessage());
            throw new IllegalArgumentException("유효하지 않은 토큰입니다", e);
        }
    }

    /**
     * 토큰에서 username 추출
     */
    public String getUsernameFromToken(String token) {
        try {
            Claims claims = getAllClaimsFromToken(token);
            return claims.get("username", String.class);
        } catch (Exception e) {
            log.error("토큰에서 username 추출 실패: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 토큰에서 이름 추출
     */
    public String getNameFromToken(String token) {
        try {
            Claims claims = getAllClaimsFromToken(token);
            return claims.get("name", String.class);
        } catch (Exception e) {
            log.error("토큰에서 이름 추출 실패: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 토큰에서 role 추출
     */
    public String getRoleFromToken(String token) {
        try {
            Claims claims = getAllClaimsFromToken(token);
            return claims.get("role", String.class);
        } catch (Exception e) {
            log.error("토큰에서 role 추출 실패: {}", e.getMessage());
            return "USER"; // 기본값 반환
        }
    }

    /**
     * 토큰 타입 확인 (access/refresh)
     */
    public String getTokenType(String token) {
        try {
            Claims claims = getAllClaimsFromToken(token);
            return claims.get("type", String.class);
        } catch (Exception e) {
            log.error("토큰 타입 확인 실패: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 토큰에서 만료일 추출
     */
    public Date getExpirationDateFromToken(String token) {
        try {
            return getClaimFromToken(token, Claims::getExpiration);
        } catch (Exception e) {
            log.error("토큰 만료일 추출 실패: {}", e.getMessage());
            throw new IllegalArgumentException("유효하지 않은 토큰입니다", e);
        }
    }

    /**
     * 토큰에서 특정 클레임 추출
     */
    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    /**
     * 토큰에서 모든 클레임 추출
     */
    private Claims getAllClaimsFromToken(String token) {
        try {
            if (token == null || token.trim().isEmpty()) {
                throw new IllegalArgumentException("토큰이 null이거나 비어있습니다");
            }

            // Bearer 토큰인 경우 처리
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            log.error("토큰 파싱 실패: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid JWT token", e);
        }
    }

    /**
     * 토큰 만료 여부 확인
     */
    private Boolean isTokenExpired(String token) {
        try {
            final Date expiration = getExpirationDateFromToken(token);
            return expiration.before(new Date());
        } catch (Exception e) {
            log.error("토큰 만료 확인 실패: {}", e.getMessage());
            return true; // 오류 발생 시 만료된 것으로 간주
        }
    }

    /**
     * 액세스 토큰 유효성 검증
     */
    public Boolean validateAccessToken(String token) {
        try {
            if (token == null || token.trim().isEmpty()) {
                log.warn("토큰이 null이거나 비어있습니다");
                return false;
            }

            String tokenType = getTokenType(token);
            if (!"access".equals(tokenType)) {
                log.warn("액세스 토큰이 아님: {}", tokenType);
                return false;
            }

            boolean isValid = !isTokenExpired(token);
            log.debug("액세스 토큰 검증 결과: {}", isValid);
            return isValid;
        } catch (Exception e) {
            log.error("액세스 토큰 검증 실패: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 리프레시 토큰 유효성 검증
     */
    public Boolean validateRefreshToken(String token) {
        try {
            if (token == null || token.trim().isEmpty()) {
                log.warn("리프레시 토큰이 null이거나 비어있습니다");
                return false;
            }

            String tokenType = getTokenType(token);
            if (!"refresh".equals(tokenType)) {
                log.warn("리프레시 토큰이 아님: {}", tokenType);
                return false;
            }

            boolean isValid = !isTokenExpired(token);
            log.debug("리프레시 토큰 검증 결과: {}", isValid);
            return isValid;
        } catch (Exception e) {
            log.error("리프레시 토큰 검증 실패: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 토큰 갱신 (리프레시 토큰으로 새 액세스 토큰 생성)
     */
    public String refreshAccessToken(String refreshToken) {
        try {
            if (!validateRefreshToken(refreshToken)) {
                throw new IllegalArgumentException("유효하지 않은 리프레시 토큰입니다");
            }

            Claims claims = getAllClaimsFromToken(refreshToken);
            String userId = claims.getSubject();

            if (userId == null || userId.trim().isEmpty()) {
                throw new IllegalArgumentException("리프레시 토큰에서 사용자 ID를 찾을 수 없습니다");
            }

            // 새로운 액세스 토큰 생성 (기본 역할로 생성)
            String newAccessToken = generateToken(userId, "USER");
            log.info("토큰 갱신 성공: userId={}", userId);

            return newAccessToken;

        } catch (Exception e) {
            log.error("토큰 갱신 실패: {}", e.getMessage());
            throw new IllegalArgumentException("토큰 갱신에 실패했습니다", e);
        }
    }

    /**
     * 보안을 위한 토큰 검증 (사용자 정보 포함하지 않음)
     */
    public Boolean validateToken(String token) {
        return validateAccessToken(token);
    }
}