package org.kosa.userservice;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;

// USER-SERVICE의 JwtUtil.java

@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret-key:mySecretKeyForJWTTokenThatShouldBeLongEnoughForSecurity}")
    private String secretKey;

    private Key key;

    @PostConstruct
    private void init() {
        // 키가 충분히 긴지 확인 (최소 32바이트)
        if (secretKey.length() < 32) {
            secretKey = "mySecretKeyForJWTTokenThatShouldBeLongEnoughForSecurity";
        }

        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        this.key = Keys.hmacShaKeyFor(keyBytes);

        log.info("USER-SERVICE JWT 유틸리티 초기화 완료");
    }

    /**
     * 🔥 토큰에서 사용자명(userId) 추출 - username 필드 우선 사용
     */
    public String getUsernameFromToken(String token) {
        try {
            Claims claims = parseToken(token);

            log.info("=== JWT 토큰 클레임 디버깅 ===");
            log.info("Subject: {}", claims.getSubject());
            log.info("Username claim: {}", claims.get("username", String.class));
            log.info("All Claims: {}", claims);
            log.info("==============================");

            // 🔥 1순위: username 필드에서 추출 (AUTH-SERVICE에서 설정한 값)
            String username = claims.get("username", String.class);
            if (username != null && !username.trim().isEmpty()) {
                log.info("username 필드에서 userId 추출 성공: {}", username);
                return username;
            }

            // 🔥 2순위: subject에서 추출 (혹시 userId가 여기 있을 경우)
            String subject = claims.getSubject();
            if (subject != null && !subject.trim().isEmpty()) {
                log.info("subject 필드에서 userId 추출 성공: {}", subject);
                return subject;
            }

            // 🔥 3순위: userId 클레임에서 추출
            String userId = claims.get("userId", String.class);
            if (userId != null && !userId.trim().isEmpty()) {
                log.info("userId 필드에서 userId 추출 성공: {}", userId);
                return userId;
            }

            log.error("모든 필드에서 userId를 찾을 수 없음");
            return null;

        } catch (Exception e) {
            log.error("JWT 토큰 파싱 오류: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 토큰 유효성 검증
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            log.debug("JWT 토큰 유효성 검증 성공");
            return true;
        } catch (Exception e) {
            log.error("JWT 토큰 유효성 검증 실패: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 토큰 파싱
     */
    private Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 🔥 토큰에서 사용자 ID 추출 (Long 타입으로)
     */
    public Long getUserIdFromToken(String token) {
        try {
            Claims claims = parseToken(token);
            String userIdStr = claims.getSubject();
            if (userIdStr != null) {
                return Long.valueOf(userIdStr);
            }
            return null;
        } catch (Exception e) {
            log.error("토큰에서 사용자 ID 추출 실패: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 🔥 토큰에서 이름 추출
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
     * 🔥 토큰에서 이메일 추출
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
     * 🔥 토큰에서 휴대폰 추출
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
}