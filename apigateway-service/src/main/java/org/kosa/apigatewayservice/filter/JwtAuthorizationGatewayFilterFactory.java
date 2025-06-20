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

            log.info("JWT Filter - Path: {}, Method: {}", path, method);

            // CORS Preflight 요청은 통과
            if (method == HttpMethod.OPTIONS) {
                log.info("CORS Preflight request - allowing: {}", path);
                return chain.filter(exchange);
            }

            // 🔥 공개 경로는 JWT 검증 스킵 (더 구체적으로 정의)
            if (isPublicPath(path, method)) {
                log.info("Public path accessed: {} [{}]", path, method);
                return chain.filter(exchange);
            }

            // Authorization 헤더 확인
            String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("Missing or invalid Authorization header for path: {} [{}]", path, method);
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

                // 사용자 정보를 헤더에 추가
                ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                        .header("X-User-Id", claims.getSubject())
                        .header("X-User-Name", claims.get("name", String.class))
                        .header("X-User-Email", claims.get("email", String.class))
                        .header("X-User-Phone", claims.get("phone", String.class))
                        .header("X-User-Role", "USER")
                        .build();

                log.info("JWT validated successfully for user: {}", claims.getSubject());

                return chain.filter(exchange.mutate().request(modifiedRequest).build());

            } catch (JwtException | IllegalArgumentException e) {
                log.error("JWT validation failed for path: {}, error: {}", path, e.getMessage());
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }
        };
    }

    /**
     * 🔥 공개 경로 판단 (메서드별로 세분화) - ✅ 주문 조회 GET 추가
     */
    private boolean isPublicPath(String path, HttpMethod method) {
        // 🔥 인증/회원가입 관련 경로
        if (path.startsWith("/auth/") || path.startsWith("/api/auth/")) {
            return true;
        }

        // ✅ 주문 서비스 공개 경로 확장
        if (path.equals("/api/orders/checkout") && (method == HttpMethod.GET || method == HttpMethod.POST)) {
            return true;
        }

        // 🔧 주문 상세 조회도 공개 (주문 완료 페이지에서 사용)
        if (path.startsWith("/api/orders/") && method == HttpMethod.GET) {
            log.info("Order detail GET request allowed: {}", path);
            return true;
        }

        if (path.startsWith("/api/checkout/") && (method == HttpMethod.GET || method == HttpMethod.POST)) {
            return true;
        }


        return false;
    }
}