package org.kosa.apigatewayservice.filter;


import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;


@Component
public class JwtAuthorizationFilter implements WebFilter {

    @Value("${jwt.secret-key}")
    private String secretKey;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // ✅ 인증 없이 접근 가능한 경로 목록
        if (path.startsWith("/api/auth/") ||
                path.equals("/auth/login") ||
                path.equals("/auth/register") ||
                path.equals("/api/users/search") || // 👉 이 라인 추가
                path.equals("/api/users/**") ||
                path.equals("/api/users") ||
                path.equals("/api/users/register") ||
                path.startsWith("/api/users/checkUserid")) {
            return chain.filter(exchange); // 인증 없이 통과
        }

        // ✅ JWT 토큰 검사
        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7);

        try {
            Jwts.parser()
                    .setSigningKey(secretKey.getBytes())
                    .parseClaimsJws(token);

            return chain.filter(exchange);

        } catch (JwtException | IllegalArgumentException e) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }
}