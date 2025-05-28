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
        // Authorization 헤더에서 JWT 토큰 추출
        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7);

        try {
            // JWT 파싱 및 서명 검증
            Jwts.parser()
                    .setSigningKey(secretKey.getBytes())  // secretKey를 byte[]로 변환해 사용
                    .parseClaimsJws(token);

            // 토큰 검증 성공 -> 다음 필터로 넘어감
            return chain.filter(exchange);

        } catch (JwtException | IllegalArgumentException e) {
            // 서명 불일치, 만료 등 예외 발생 시
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }
}