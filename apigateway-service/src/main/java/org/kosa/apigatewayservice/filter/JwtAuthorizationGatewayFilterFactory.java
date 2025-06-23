package org.kosa.apigatewayservice.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
@Slf4j
public class JwtAuthorizationGatewayFilterFactory extends AbstractGatewayFilterFactory<JwtAuthorizationGatewayFilterFactory.Config> {

    @Value("${jwt.secret-key}")
    private String secretKey;

    public JwtAuthorizationGatewayFilterFactory() {
        super(Config.class);
    }

    public static class Config {
        // 필요시 설정값 추가
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getURI().getPath();
            HttpMethod method = exchange.getRequest().getMethod();

            log.info("🔐 JWT Authorization Filter - Path: {}, Method: {}", path, method);

            // CORS Preflight 요청은 통과
            if (method == HttpMethod.OPTIONS) {
                log.info("✅ CORS Preflight request - allowing: {}", path);
                return chain.filter(exchange);
            }

            // Authorization 헤더 확인
            String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("❌ Missing or invalid Authorization header for path: {} [{}]", path, method);
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            String token = authHeader.substring(7);

            try {
                SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));

                Claims claims = Jwts.parserBuilder()
                        .setSigningKey(key)
                        .build()
                        .parseClaimsJws(token)
                        .getBody();

                // 🔥 사용자 정보 추출 (문자열 subject 완벽 지원)
                String subject = claims.getSubject();
                String username = claims.get("username", String.class);
                String name = claims.get("name", String.class);
                String email = claims.get("email", String.class);
                String phone = claims.get("phone", String.class);

                // 🔥 userId 처리: subject를 그대로 사용 (숫자든 문자열이든)
                String userId = subject != null ? subject : username;
                String finalUsername = username != null ? username : subject;

                if (userId == null) {
                    log.error("❌ JWT에서 사용자 식별자를 찾을 수 없음");
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                }

                log.info("✅ JWT validated - Subject: '{}', Username: '{}', Final UserId: '{}'", subject, username, userId);

                // 🔥 사용자 정보를 헤더에 추가
                ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                        .header("X-User-Id", userId)
                        .header("X-Username", finalUsername != null ? finalUsername : userId)
                        .header("X-User-Name", name != null ? name : "")
                        .header("X-User-Email", email != null ? email : "")
                        .header("X-User-Phone", phone != null ? phone : "")
                        .header("X-User-Role", "USER")
                        .build();

                log.info("🎯 JWT Authorization 성공 - X-User-Id: '{}', X-Username: '{}'", userId, finalUsername);

                return chain.filter(exchange.mutate().request(modifiedRequest).build());

            } catch (JwtException | IllegalArgumentException e) {
                log.error("❌ JWT validation failed for path: {}, error: {}", path, e.getMessage());
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }
        };
    }
}