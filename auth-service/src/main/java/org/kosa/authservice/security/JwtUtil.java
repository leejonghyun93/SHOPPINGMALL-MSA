package org.kosa.authservice.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.LocalDateTime;
import java.util.Date;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;


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

        log.info("JWT 유틸리티 초기화 완료");
    }

    /**
     * 토큰 생성
     */
    public String generateToken(Long userId, String username, String name, String email, String phone) {
        Date now = new Date();
        Date expireDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .setSubject(String.valueOf(userId))  // 🔥 Gateway에서 claims.getSubject()로 사용자 ID를 가져옴

                .claim("username", username)         // username은 별도 claim으로
                .claim("name", name)                 // Gateway에서 claims.get("name", String.class)로 사용
                .claim("email", email)               // Gateway에서 claims.get("email", String.class)로 사용
                .claim("phone", phone)               // Gateway에서 claims.get("phone", String.class)로 사용
                .claim("role", "USER")               // 기본 역할 추가 (필요시)
                .setIssuer("auth-service")
                .setIssuedAt(now)
                .setExpiration(expireDate)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 토큰에서 사용자명 추출 🔥 수정됨
     */
    public String getUsernameFromToken(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.get("username", String.class);  // 🔥 subject가 아닌 username claim에서 가져옴
        } catch (Exception e) {
            log.error("토큰에서 사용자명 추출 실패: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 토큰에서 사용자 ID 추출 🔥 수정됨
     */
    public Long getUserIdFromToken(String token) {
        try {
            Claims claims = parseToken(token);
            String userIdStr = claims.getSubject();  // 🔥 subject에서 userId를 가져옴
            return Long.valueOf(userIdStr);
        } catch (Exception e) {
            log.error("토큰에서 사용자 ID 추출 실패: {}", e.getMessage());
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
     * 🔥 새로 추가: 토큰에서 이메일 추출
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
     * 🔥 새로 추가: 토큰에서 휴대폰 추출
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
     * 만료된 토큰에서 사용자명 추출 (토큰 갱신용) 🔥 수정됨
     */
    public String getUsernameFromExpiredToken(String token) {
        try {
            Claims claims = parseExpiredToken(token);
            return claims.get("username", String.class);  // 🔥 subject가 아닌 username claim에서 가져옴
        } catch (Exception e) {
            log.error("만료된 토큰에서 사용자명 추출 실패: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 만료된 토큰에서 사용자 ID 추출 (토큰 갱신용) 🔥 수정됨
     */
    public Long getUserIdFromExpiredToken(String token) {
        try {
            Claims claims = parseExpiredToken(token);
            String userIdStr = claims.getSubject();  // 🔥 subject에서 userId를 가져옴
            return Long.valueOf(userIdStr);
        } catch (Exception e) {
            log.error("만료된 토큰에서 사용자 ID 추출 실패: {}", e.getMessage());
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
     * 🔥 새로 추가: 만료된 토큰에서 이메일 추출
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
     * 🔥 새로 추가: 만료된 토큰에서 휴대폰 추출
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