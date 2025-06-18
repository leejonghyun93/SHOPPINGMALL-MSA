package org.kosa.apigatewayservice.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
@Slf4j
public class SimpleJwtFilter extends AbstractGatewayFilterFactory<SimpleJwtFilter.Config> {

    @Value("${jwt.secret-key}")
    private String secretKey;

    public SimpleJwtFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            // Authorization 헤더 확인
            String authHeader = request.getHeaders().getFirst("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                try {
                    String token = authHeader.substring(7);

                    // 🔧 Auth Service와 동일한 키 생성 방식
                    String actualSecretKey = secretKey;
                    if (secretKey.length() < 32) {
                        actualSecretKey = "mySecretKeyForJWTTokenThatShouldBeLongEnoughForSecurity";
                    }

                    SecretKey key = Keys.hmacShaKeyFor(actualSecretKey.getBytes(StandardCharsets.UTF_8));
                    Claims claims = Jwts.parserBuilder()
                            .setSigningKey(key)
                            .build()
                            .parseClaimsJws(token)
                            .getBody();

                    // 🔧 Auth Service JWT 구조에 맞게 정보 추출
                    String username = claims.getSubject();              // username (asdasds)
                    Long userId = claims.get("userId", Long.class);     // 실제 사용자 ID
                    String name = claims.get("name", String.class);     // 실제 이름
                    String issuer = claims.getIssuer();                 // auth-service

                    log.info("✅ JWT 파싱 성공 - Username: {}, UserID: {}, Name: {}", username, userId, name);

                    // 🔧 다운스트림 서비스로 모든 사용자 정보 전달
                    ServerHttpRequest mutatedRequest = request.mutate()
                            .header("X-User-Id", String.valueOf(userId))        // Long → String 변환
                            .header("X-Username", username != null ? username : "")
                            .header("X-User-Name", name != null ? name : "")
                            .header("Authorization", authHeader) // 원본 토큰도 유지
                            .build();

                    return chain.filter(exchange.mutate().request(mutatedRequest).build());

                } catch (Exception e) {
                    log.warn("⚠️ JWT 파싱 실패, 원본 요청 전달: {}", e.getMessage());
                    // JWT 파싱 실패해도 요청은 계속 진행 (Cart Service에서 게스트로 처리)
                }
            } else {
                log.info("🔓 Authorization 헤더 없음 - 게스트 요청으로 처리");
            }

            // JWT가 없거나 파싱 실패 시 원본 요청 그대로 전달
            return chain.filter(exchange);
        };
    }

    public static class Config {
        // 설정 클래스
    }
}