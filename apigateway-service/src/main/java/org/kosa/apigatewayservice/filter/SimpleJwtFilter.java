package org.kosa.apigatewayservice.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.util.Collections;
import java.util.List;

@Component
public class SimpleJwtFilter implements WebFilter {

    // 🔥 Auth Service와 동일한 JWT Secret 사용
    @Value("${jwt.secret:verySecretKeyThatIsAtLeast32BytesLong1234}")
    private String jwtSecret;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        // 🔥 완전 공개 경로는 JWT 검증 스킵
        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        // 🔥 Authorization 헤더가 없는 경우
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // 인증이 필요한 경로인 경우 401 반환
            if (isAuthRequiredPath(path)) {
                return handleUnauthorized(exchange, "Authorization header missing");
            }
            // 그 외에는 통과
            return chain.filter(exchange);
        }

        String token = authHeader.substring(7);

        try {
            // 🔥 JWT 토큰 파싱 및 검증
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String userId = claims.getSubject();
            String role = claims.get("role", String.class);
            String name = claims.get("name", String.class);
            String email = claims.get("email", String.class);
            String phone = claims.get("phone", String.class);

            // 🔥 요청 헤더에 사용자 정보 추가
            ServerHttpRequest modifiedRequest = request.mutate()
                    .header("X-User-Id", userId)
                    .header("X-User-Role", role)
                    .header("X-User-Name", name)
                    .header("X-User-Email", email != null ? email : "")
                    .header("X-User-Phone", phone != null ? phone : "")
                    .build();

            // 🔥 Spring Security Context에 인증 정보 설정
            List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                    new SimpleGrantedAuthority("ROLE_" + (role != null ? role : "USER"))
            );

            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(userId, null, authorities);

            return chain.filter(exchange.mutate().request(modifiedRequest).build())
                    .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authToken));

        } catch (Exception e) {
            // 🔥 JWT 파싱 실패
            System.err.println("JWT 파싱 실패: " + e.getMessage());

            // 인증이 필요한 경로인 경우 401 반환
            if (isAuthRequiredPath(path)) {
                return handleUnauthorized(exchange, "Invalid JWT token: " + e.getMessage());
            }

            // 그 외에는 인증 없이 통과
            return chain.filter(exchange);
        }
    }

    // 🔥 완전 공개 경로 확인
    private boolean isPublicPath(String path) {
        return path.startsWith("/auth/") ||
                path.startsWith("/api/users/register") ||
                path.startsWith("/api/users/login") ||
                path.startsWith("/api/users/verify-password") ||
                path.startsWith("/api/users/checkUserId/") ||
                path.startsWith("/api/users/health") ||
                path.startsWith("/api/categories/") ||
                path.startsWith("/api/products/") ||
                path.startsWith("/api/broadcasts/") ||
                path.startsWith("/api/cart/guest/") ||
                path.startsWith("/api/payments/guest/") ||
                path.startsWith("/api/payments/webhook") ||
                path.startsWith("/api/images/") ||
                path.startsWith("/images/") ||
                path.startsWith("/actuator/health/");
    }

    // 🔥 인증이 필요한 경로 확인
    private boolean isAuthRequiredPath(String path) {
        return path.startsWith("/api/users/profile") ||
                path.startsWith("/api/users/points") ||
                path.startsWith("/api/users/coupons") ||
                path.startsWith("/api/users/addresses") ||
                path.startsWith("/api/users/withdraw") ||
                path.startsWith("/api/cart/") && !path.startsWith("/api/cart/guest/") ||
                path.startsWith("/api/orders/") ||
                path.startsWith("/api/payments/") && !path.startsWith("/api/payments/guest/") && !path.startsWith("/api/payments/webhook");
    }

    // 🔥 401 Unauthorized 응답 처리
    private Mono<Void> handleUnauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().add("Content-Type", "application/json");

        String responseBody = String.format(
                "{\"success\": false, \"message\": \"%s\", \"code\": \"UNAUTHORIZED\"}",
                message
        );

        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(responseBody.getBytes()))
        );
    }
}