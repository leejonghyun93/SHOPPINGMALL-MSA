package org.kosa.userservice.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
@Slf4j
public class JwtTokenParser {

    @Value("${jwt.secret:verySecretKeyThatIsAtLeast32BytesLong1234}")
    private String jwtSecret;

    private SecretKey getSigningKey() {
        if (jwtSecret.length() < 32) {
            log.warn("JWT secret key가 너무 짧습니다. 최소 32바이트 필요");
            jwtSecret = "verySecretKeyThatIsAtLeast32BytesLong1234567890";
        }
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 🔥 순수 JWT 방식: 토큰에서 사용자 ID 추출
     * Auth-Service 호출이 불가능한 경우에만 사용
     */
    public String extractUserIdFromToken(String token) {
        try {
            if (token == null || token.trim().isEmpty()) {
                return null;
            }

            // Bearer 토큰인 경우 처리
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            // 사용자 ID 추출 (Auth-Service와 동일한 로직)
            String userId = claims.getSubject();
            if (isValidUserId(userId)) {
                return userId;
            }

            userId = claims.get("username", String.class);
            if (isValidUserId(userId)) {
                return userId;
            }

            userId = claims.get("userId", String.class);
            if (isValidUserId(userId)) {
                return userId;
            }

            return null;

        } catch (Exception e) {
            log.error("JWT 토큰 파싱 실패: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 토큰 유효성 검증
     */
    public boolean validateToken(String token) {
        try {
            if (token == null || token.trim().isEmpty()) {
                return false;
            }

            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);

            return true;
        } catch (Exception e) {
            log.debug("JWT 토큰 검증 실패: {}", e.getMessage());
            return false;
        }
    }

    private boolean isValidUserId(String userId) {
        return userId != null &&
                !userId.trim().isEmpty() &&
                !"null".equals(userId) &&
                !"undefined".equals(userId);
    }
}