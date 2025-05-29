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
        // 필요 시 필터 설정값 추가
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getURI().getPath();
            HttpMethod method = exchange.getRequest().getMethod();

            // ✅ OPTIONS 요청은 무조건 통과 (CORS preflight)
            if (method == HttpMethod.OPTIONS) {
                return chain.filter(exchange);
            }

            // ✅ 인증을 제외할 경로들 (더 정확한 매칭)
            if (isPublicPath(path)) {
                return chain.filter(exchange);
            }

            // ✅ Authorization 헤더 검증
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

                // 사용자 정보를 헤더에 추가 (선택사항)
                // exchange.getRequest().mutate()
                //     .header("X-User-Id", claims.getSubject())
                //     .build();

                return chain.filter(exchange);

            } catch (JwtException | IllegalArgumentException e) {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }
        };
    }

    /**
     * 공개 경로인지 확인
     */
    private boolean isPublicPath(String path) {
        return path.equals("/api/users/register") ||
                path.equals("/api/users/checkUserid") ||
                path.startsWith("/auth/") ||
                path.startsWith("/api/auth/") ||
                path.equals("/api/users/search"); // 👉 이 라인 추가

    }
}