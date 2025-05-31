package org.kosa.apigatewayservice.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class JwtAuthorizationGatewayFilterFactory extends AbstractGatewayFilterFactory<JwtAuthorizationGatewayFilterFactory.Config> {

    @Value("${jwt.secret-key}")
    private String secretKey;

    public JwtAuthorizationGatewayFilterFactory() {
        super(Config.class);
    }

    public static class Config {
        // 설정값이 필요하면 추가
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getURI().getPath();
            HttpMethod method = exchange.getRequest().getMethod();

            // ✅ CORS Preflight 요청은 통과
            if (method == HttpMethod.OPTIONS) {
                return chain.filter(exchange);
            }

            // ✅ 인증 없이 접근 가능한 공개 경로는 통과
            if (isPublicPath(path)) {
                return chain.filter(exchange);
            }

            // ✅ Authorization 헤더 확인
            String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            String token = authHeader.substring(7);

            try {
                Claims claims = Jwts.parser()
                        .setSigningKey(secretKey.getBytes())
                        .parseClaimsJws(token)
                        .getBody();

                // 필요한 경우 사용자 ID를 헤더에 추가
                // exchange.getRequest().mutate()
                //         .header("X-User-Id", claims.getSubject())
                //         .build();

                return chain.filter(exchange);

            } catch (JwtException | IllegalArgumentException e) {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }
        };
    }

    /**
     * 공개 접근 경로 목록
     */
    private boolean isPublicPath(String path) {
        return path.equals("/api/users/register") ||
                path.equals("/api/users") || // 루트도 허용
                path.equals("/api/users/list") ||
                path.startsWith("/api/users/checkUserid") ||
                path.equals("/api/users/search") ||
                path.startsWith("/auth/") ||
                path.startsWith("/api/auth/");
    }
}
