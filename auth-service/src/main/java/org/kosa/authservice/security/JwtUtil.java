package org.kosa.authservice.util;

import io.jsonwebtoken.Claims;
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

    @Value("${jwt.secret:verySecretKeyThatIsAtLeast32BytesLong1234}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400000}") // 24시간 (밀리초)
    private Long jwtExpiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 🔒 보안 강화된 JWT 토큰 생성 - 최소한의 정보만 포함
     */
    public String generateToken(String userId, String username, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);

        log.info("🔍 JWT 토큰 생성 - userId: '{}', username: '{}', role: '{}'", userId, username, role);

        return Jwts.builder()
                .setSubject(userId) // 사용자 식별자를 subject로 설정
                .claim("username", username) // 🔒 username은 포함 (로그인 ID, 상대적으로 덜 민감)
                .claim("role", role != null ? role : "USER")
                .claim("type", "access") // 토큰 타입
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .setIssuer("auth-service") // 발급자
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 🔒 리프레시 토큰 생성 (더 긴 만료시간, 최소 정보)
     */
    public String generateRefreshToken(String userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + (jwtExpiration * 7)); // 7일

        return Jwts.builder()
                .setSubject(userId)
                .claim("type", "refresh")
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .setIssuer("auth-service")
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 토큰에서 사용자 ID 추출
     */
    public String getUserIdFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    /**
     * 토큰에서 username 추출
     */
    public String getUsernameFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return claims.get("username", String.class);
    }

    /**
     * 토큰에서 role 추출
     */
    public String getRoleFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return claims.get("role", String.class);
    }

    /**
     * 토큰 타입 확인 (access/refresh)
     */
    public String getTokenType(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return claims.get("type", String.class);
    }

    /**
     * 토큰에서 만료일 추출
     */
    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
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
        final Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }

    /**
     * 액세스 토큰 유효성 검증
     */
    public Boolean validateAccessToken(String token) {
        try {
            String tokenType = getTokenType(token);
            if (!"access".equals(tokenType)) {
                log.warn("액세스 토큰이 아님: {}", tokenType);
                return false;
            }
            return !isTokenExpired(token);
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
            String tokenType = getTokenType(token);
            if (!"refresh".equals(tokenType)) {
                log.warn("리프레시 토큰이 아님: {}", tokenType);
                return false;
            }
            return !isTokenExpired(token);
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

            // 새로운 액세스 토큰 생성 (username과 role은 기존 토큰에서 추출하거나 기본값 사용)
            String username = claims.get("username", String.class);
            return generateToken(userId, username != null ? username : userId, "USER");

        } catch (Exception e) {
            log.error("토큰 갱신 실패: {}", e.getMessage());
            throw new IllegalArgumentException("토큰 갱신에 실패했습니다", e);
        }
    }

    /**
     * 🔒 보안을 위한 토큰 검증 (사용자 정보 포함하지 않음)
     */
    public Boolean validateToken(String token) {
        return validateAccessToken(token);
    }

}