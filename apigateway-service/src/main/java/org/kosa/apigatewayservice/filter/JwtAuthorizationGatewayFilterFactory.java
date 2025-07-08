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
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
@Slf4j
public class JwtAuthorizationGatewayFilterFactory extends AbstractGatewayFilterFactory<JwtAuthorizationGatewayFilterFactory.Config> {

    @Value("${jwt.secret:rrYd2zPDUkx7BUhgDsOTxHCbsBkeTgE/uoARWYSqBjU=}")
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

            log.info("🔍 JWT Authorization Filter - Path: {}, Method: {}", path, method);

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

            String jwt = authHeader.substring(7);

            try {
                SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));

                Claims claims = Jwts.parserBuilder()
                        .setSigningKey(key)
                        .build()
                        .parseClaimsJws(jwt)
                        .getBody();

                // 사용자 정보 추출 (검증용)
                String subject = claims.getSubject();
                String username = claims.get("username", String.class);
                String userId = subject != null ? subject : username;

                if (userId == null) {
                    log.error("❌ JWT에서 사용자 식별자를 찾을 수 없음");
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                }

                log.info("✅ JWT validated - Subject: '{}', Username: '{}', Final UserId: '{}'", subject, username, userId);

                // 🔥 X-헤더 생성 제거 - 원본 요청 그대로 전달
                // 백엔드 서비스들이 각자 JWT 토큰을 파싱하여 사용자 정보 추출
                log.info("✅ JWT Authorization 성공 - 원본 요청 그대로 전달 (X-헤더 없이): {}", path);

                return chain.filter(exchange);

            } catch (JwtException | IllegalArgumentException e) {
                log.error("❌ JWT validation failed for path: {}, error: {}", path, e.getMessage());
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }
        };
    }
}