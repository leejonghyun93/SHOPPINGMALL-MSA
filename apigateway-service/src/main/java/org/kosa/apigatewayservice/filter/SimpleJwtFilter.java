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

                    SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
                    Claims claims = Jwts.parserBuilder()
                            .setSigningKey(key)
                            .build()
                            .parseClaimsJws(token)
                            .getBody();

                    String userIdStr = claims.getSubject();
                    String username = claims.get("username", String.class);
                    String name = claims.get("name", String.class);
                    String email = claims.get("email", String.class);
                    String phone = claims.get("phone", String.class);

                    log.info("✅ JWT 파싱 성공 - UserID: {}, Username: {}", userIdStr, username);

                    // 헤더에 사용자 정보 추가
                    ServerHttpRequest mutatedRequest = request.mutate()
                            .header("X-User-Id", userIdStr)
                            .header("X-Username", username != null ? username : "")
                            .header("X-User-Name", name != null ? name : "")
                            .header("X-User-Email", email != null ? email : "")
                            .header("X-User-Phone", phone != null ? phone : "")
                            .header("Authorization", authHeader)
                            .build();

                    return chain.filter(exchange.mutate().request(mutatedRequest).build());

                } catch (Exception e) {
                    log.warn("⚠️ JWT 파싱 실패하지만 요청 전달: {}", e.getMessage());
                    // 🔥 인증 실패해도 요청 전달 (USER-SERVICE에서 검증)
                    return chain.filter(exchange);
                }
            } else {
                log.info("🔓 Authorization 헤더 없음");
            }

            return chain.filter(exchange);
        };
    }

    // 🔥 이 부분을 추가하세요!
    public static class Config {
        // 설정 클래스 (현재는 비어있음)
    }
}