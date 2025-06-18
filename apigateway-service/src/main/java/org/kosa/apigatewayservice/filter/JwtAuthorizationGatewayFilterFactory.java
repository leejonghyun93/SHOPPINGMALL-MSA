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
     * 🔥 공개 경로 판단 (메서드별로 세분화)
     */
    private boolean isPublicPath(String path, HttpMethod method) {
        // 🔥 인증/회원가입 관련 경로
        if (path.startsWith("/auth/") || path.startsWith("/api/auth/")) {
            return true;
        }

        // 🔥 사용자 서비스 공개 경로
        if (path.equals("/api/users/register") && method == HttpMethod.POST) {
            return true;
        }
        if (path.equals("/api/users/checkUserId") || path.startsWith("/api/users/checkUserId")) {
            return true;
        }
        if (path.equals("/api/users/health") || path.equals("/api/users/list")) {
            return true;
        }
        if (path.equals("/api/users/verify-password") && method == HttpMethod.POST) {
            return true;
        }
        if (path.equals("/api/users/profile") && (method == HttpMethod.GET || method == HttpMethod.PUT)) {
            return true;
        }

        // 🔥 카테고리 서비스 (GET 요청만 공개)
        if (path.startsWith("/api/categories") && method == HttpMethod.GET) {
            return true;
        }

        // 🔥 상품 서비스 (GET 요청만 공개)
        if (path.startsWith("/api/products") && method == HttpMethod.GET) {
            return true;
        }

        // 🛒 Cart Service 공개 경로 추가 (임시 - 실제로는 인증 필요)
        if (path.startsWith("/api/cart")) {
            log.info("Cart API accessed without authentication: {} [{}]", path, method);
            return true;  // 현재는 테스트를 위해 모든 Cart API를 공개
        }

        // 🔥 이미지 서비스 (GET 요청만 공개)
        if (path.startsWith("/api/images") && method == HttpMethod.GET) {
            return true;
        }

        // 🔥 정적 리소스 (이미지, CSS, JS 등)
        if (path.startsWith("/images/") || path.startsWith("/static/") || path.startsWith("/assets/")) {
            return true;
        }

        // 🔥 Actuator Health Check
        if (path.startsWith("/actuator/health")) {
            return true;
        }

        return false;
    }
}