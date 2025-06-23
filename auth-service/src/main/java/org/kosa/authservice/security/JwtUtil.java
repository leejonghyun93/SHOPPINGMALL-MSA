package org.kosa.authservice.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
@Slf4j
public class JwtUtil {

    @Value("${jwt.secret-key:mySecretKeyForJWTTokenThatShouldBeLongEnoughForSecurity}")
    private String secretKey;

    @Value("${jwt.expiration:3600000}") // 1시간
    private long expiration;

    private Key key;

    @PostConstruct
    private void init() {
        // 키가 충분히 긴지 확인 (최소 32바이트)
        if (secretKey.length() < 32) {
            secretKey = "mySecretKeyForJWTTokenThatShouldBeLongEnoughForSecurity";
        }

        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        this.key = Keys.hmacShaKeyFor(keyBytes);

        log.info("JWT 유틸리티 초기화 완료 - SecretKey 길이: {}", secretKey.length());
    }

    /**
     * 토큰 생성 - 🔥 문자열 userId 완벽 지원
     */
    public String generateToken(Long userId, String username, String name, String email, String phone) {
        Date now = new Date();
        Date expireDate = new Date(now.getTime() + expiration);

        // 🔥 subject 결정 로직: userId가 있으면 숫자로, 없으면 username을 subject로 사용
        String subject;
        if (userId != null) {
            subject = String.valueOf(userId);  // 숫자 ID를 문자열로
            log.info("🔥 JWT 생성 - 숫자 userId를 subject로 사용: '{}'", subject);
        } else if (username != null && !username.trim().isEmpty()) {
            subject = username;  // 문자열 username을 subject로
            log.info("🔥 JWT 생성 - username을 subject로 사용: '{}'", subject);
        } else {
            throw new IllegalArgumentException("userId와 username 모두 null일 수 없습니다");
        }

        log.info("🎯 JWT 토큰 생성 중 - Subject: '{}', Username: '{}', UserId: {}", subject, username, userId);

        String token = Jwts.builder()
                .setSubject(subject)  // 🔥 userId 또는 username
                .claim("username", username)
                .claim("name", name)
                .claim("email", email)
                .claim("phone", phone)
                .claim("role", "USER")
                .setIssuer("auth-service")
                .setIssuedAt(now)
                .setExpiration(expireDate)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        log.info("✅ JWT 토큰 생성 완료 - 토큰 길이: {}", token.length());
        return token;
    }

    /**
     * 토큰에서 subject 추출 (문자열 또는 숫자)
     */
    public String getSubjectFromToken(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.getSubject();
        } catch (Exception e) {
            log.error("토큰에서 subject 추출 실패: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 토큰에서 사용자명 추출
     */
    public String getUsernameFromToken(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.get("username", String.class);
        } catch (Exception e) {
            log.error("토큰에서 사용자명 추출 실패: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 토큰에서 사용자 ID 추출 - 🔥 문자열도 지원하도록 수정
     */
    public Long getUserIdFromToken(String token) {
        try {
            Claims claims = parseToken(token);
            String subject = claims.getSubject();

            // 🔥 숫자로 변환 가능한지 확인
            try {
                return Long.valueOf(subject);
            } catch (NumberFormatException e) {
                // 🔥 숫자가 아닌 경우 (예: "qweas") null 반환
                log.debug("토큰의 subject가 숫자가 아님: '{}'. null 반환", subject);
                return null;
            }
        } catch (Exception e) {
            log.error("토큰에서 사용자 ID 추출 실패: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 토큰에서 사용자 식별자 추출 (숫자 또는 문자열)
     */
    public String getUserIdentifierFromToken(String token) {
        try {
            Claims claims = parseToken(token);
            String subject = claims.getSubject();
            String username = claims.get("username", String.class);

            // subject가 있으면 subject를, 없으면 username을 반환
            return subject != null ? subject : username;
        } catch (Exception e) {
            log.error("토큰에서 사용자 식별자 추출 실패: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 토큰에서 이름 추출
     */
    public String getNameFromToken(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.get("name", String.class);
        } catch (Exception e) {
            log.error("토큰에서 이름 추출 실패: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 토큰에서 이메일 추출
     */
    public String getEmailFromToken(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.get("email", String.class);
        } catch (Exception e) {
            log.error("토큰에서 이메일 추출 실패: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 토큰에서 휴대폰 추출
     */
    public String getPhoneFromToken(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.get("phone", String.class);
        } catch (Exception e) {
            log.error("토큰에서 휴대폰 추출 실패: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 만료된 토큰에서 subject 추출
     */
    public String getSubjectFromExpiredToken(String token) {
        try {
            Claims claims = parseExpiredToken(token);
            return claims.getSubject();
        } catch (Exception e) {
            log.error("만료된 토큰에서 subject 추출 실패: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 만료된 토큰에서 사용자명 추출 (토큰 갱신용)
     */
    public String getUsernameFromExpiredToken(String token) {
        try {
            Claims claims = parseExpiredToken(token);
            return claims.get("username", String.class);
        } catch (Exception e) {
            log.error("만료된 토큰에서 사용자명 추출 실패: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 만료된 토큰에서 사용자 ID 추출 (토큰 갱신용) - 🔥 문자열도 지원
     */
    public Long getUserIdFromExpiredToken(String token) {
        try {
            Claims claims = parseExpiredToken(token);
            String subject = claims.getSubject();

            // 🔥 숫자로 변환 가능한지 확인
            try {
                return Long.valueOf(subject);
            } catch (NumberFormatException e) {
                // 🔥 숫자가 아닌 경우 (예: "qweas") null 반환
                log.debug("만료된 토큰의 subject가 숫자가 아님: '{}'. null 반환", subject);
                return null;
            }
        } catch (Exception e) {
            log.error("만료된 토큰에서 사용자 ID 추출 실패: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 만료된 토큰에서 사용자 식별자 추출 (숫자 또는 문자열)
     */
    public String getUserIdentifierFromExpiredToken(String token) {
        try {
            Claims claims = parseExpiredToken(token);
            String subject = claims.getSubject();
            String username = claims.get("username", String.class);

            // subject가 있으면 subject를, 없으면 username을 반환
            return subject != null ? subject : username;
        } catch (Exception e) {
            log.error("만료된 토큰에서 사용자 식별자 추출 실패: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 만료된 토큰에서 이름 추출 (토큰 갱신용)
     */
    public String getNameFromExpiredToken(String token) {
        try {
            Claims claims = parseExpiredToken(token);
            return claims.get("name", String.class);
        } catch (Exception e) {
            log.error("만료된 토큰에서 이름 추출 실패: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 만료된 토큰에서 이메일 추출
     */
    public String getEmailFromExpiredToken(String token) {
        try {
            Claims claims = parseExpiredToken(token);
            return claims.get("email", String.class);
        } catch (Exception e) {
            log.error("만료된 토큰에서 이메일 추출 실패: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 만료된 토큰에서 휴대폰 추출
     */
    public String getPhoneFromExpiredToken(String token) {
        try {
            Claims claims = parseExpiredToken(token);
            return claims.get("phone", String.class);
        } catch (Exception e) {
            log.error("만료된 토큰에서 휴대폰 추출 실패: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 토큰 유효성 검증
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            log.error("토큰 검증 실패: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 토큰 만료 여부 확인
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            // 만료된 경우
            return true;
        } catch (Exception e) {
            // 다른 에러 (형식 오류 등)
            return true;
        }
    }

    /**
     * 토큰 파싱 (유효한 토큰만)
     */
    private Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 만료된 토큰 파싱 (토큰 갱신용)
     */
    private Claims parseExpiredToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            // 만료된 토큰의 경우 Claims를 여전히 얻을 수 있음
            log.info("만료된 토큰에서 Claims 추출 성공");
            return e.getClaims();
        } catch (Exception e) {
            log.error("토큰 파싱 실패: {}", e.getMessage());
            throw e;
        }
    }
}