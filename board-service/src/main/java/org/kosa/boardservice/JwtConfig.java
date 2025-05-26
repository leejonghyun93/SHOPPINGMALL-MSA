package org.kosa.boardservice;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Configuration
public class JwtConfig {

    @Value("${jwt.secret-key}") // ❌ 에러의 원인
    private String secretKey;
    @PostConstruct
    public void init() {
        System.out.println("[JWT 시크릿 키] " + secretKey);
    }
    @Bean
    public JwtDecoder jwtDecoder() {
        try {
            // Base64로 인코딩된 키라면 디코딩, 아니라면 그대로 사용
            byte[] keyBytes;
            if (isBase64(secretKey)) {
                keyBytes = Base64.getDecoder().decode(secretKey);
            } else {
                keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
            }

            SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, "HmacSHA256");
            return NimbusJwtDecoder.withSecretKey(secretKeySpec).build();
        } catch (Exception e) {
            System.err.println("JWT 디코더 설정 오류: " + e.getMessage());
            throw new RuntimeException("JWT 디코더 설정 실패", e);
        }
    }

    private boolean isBase64(String str) {
        try {
            Base64.getDecoder().decode(str);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}