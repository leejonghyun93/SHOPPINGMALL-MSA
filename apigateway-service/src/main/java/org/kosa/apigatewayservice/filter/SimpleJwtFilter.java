// 🔥 SimpleJwtFilter.java - Q&A 경로 추가

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

    @Value("${jwt.secret:verySecretKeyThatIsAtLeast32BytesLong1234}")
    private String jwtSecret;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();
        String method = request.getMethod().name();

        System.out.println("🔍 JWT Filter - Path: " + path + ", Method: " + method);

        // 완전 공개 경로는 JWT 검증 스킵
        if (isPublicPath(path, method)) {
            System.out.println("✅ 공개 경로로 인식, JWT 검증 스킵: " + path + " (" + method + ")");
            return chain.filter(exchange);
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        // Authorization 헤더가 없는 경우
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // 인증이 필요한 경로인 경우 401 반환
            if (isAuthRequiredPath(path, method)) {
                System.out.println("❌ 인증 필요 경로인데 토큰 없음: " + path + " (" + method + ")");
                return handleUnauthorized(exchange, "Authorization header missing");
            }
            // 그 외에는 통과
            System.out.println("⚠️ 토큰 없지만 선택적 인증 경로로 통과: " + path);
            return chain.filter(exchange);
        }

        String token = authHeader.substring(7);

        try {
            // JWT 토큰 파싱 및 검증
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

            System.out.println("✅ JWT 토큰 검증 성공 - User: " + userId + ", Role: " + role);

            // 요청 헤더에 사용자 정보 추가
            ServerHttpRequest modifiedRequest = request.mutate()
                    .header("X-User-Id", userId)
                    .header("X-User-Role", role)
                    .header("X-User-Name", name)
                    .header("X-User-Email", email != null ? email : "")
                    .header("X-User-Phone", phone != null ? phone : "")
                    .build();

            // Spring Security Context에 인증 정보 설정
            List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                    new SimpleGrantedAuthority("ROLE_" + (role != null ? role : "USER"))
            );

            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(userId, null, authorities);

            return chain.filter(exchange.mutate().request(modifiedRequest).build())
                    .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authToken));

        } catch (Exception e) {
            // JWT 파싱 실패
            System.err.println("❌ JWT 파싱 실패: " + e.getMessage());

            // 인증이 필요한 경로인 경우 401 반환
            if (isAuthRequiredPath(path, method)) {
                System.out.println("❌ 인증 필요 경로인데 토큰 유효하지 않음: " + path + " (" + method + ")");
                return handleUnauthorized(exchange, "Invalid JWT token: " + e.getMessage());
            }

            // 그 외에는 인증 없이 통과
            System.out.println("⚠️ 토큰 무효하지만 선택적 인증 경로로 통과: " + path);
            return chain.filter(exchange);
        }
    }

    // 🔥 완전 공개 경로 확인 - Q&A GET 요청 추가
    private boolean isPublicPath(String path, String method) {
        // 기본 공개 경로들
        if (path.startsWith("/auth/") ||
                path.startsWith("/api/users/register") ||
                path.startsWith("/api/users/login") ||
                path.startsWith("/api/users/findId") ||
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
                path.startsWith("/upload/") ||
                path.equals("/auth/findPassword") ||
                path.equals("/auth/verifyResetCode") ||
                path.equals("/auth/resetPassword") ||
                path.startsWith("/actuator/health/")) {
            return true;
        }

        // 🔥 Board Service - GET 요청만 공개
        if (path.startsWith("/api/board/") && "GET".equals(method)) {
            return true;
        }

        // 🔥 Q&A Service - GET 요청만 공개
        if (path.startsWith("/api/qna/") && "GET".equals(method)) {
            return true;
        }

        // 알림 서비스 공개 경로
        if (path.startsWith("/api/notifications/health") ||
                path.startsWith("/api/notifications/broadcasts/")) {
            return true;
        }

        return false;
    }

    // 🔥 인증이 필요한 경로 확인 - Q&A CUD 작업 추가
    private boolean isAuthRequiredPath(String path, String method) {
        // 사용자 관련 인증 필요 경로
        if (path.startsWith("/api/users/profile") ||
                path.startsWith("/api/users/points") ||
                path.startsWith("/api/users/coupons") ||
                path.startsWith("/api/users/addresses") ||
                path.startsWith("/api/users/withdraw")) {
            return true;
        }

        // 장바구니 (게스트 제외)
        if (path.startsWith("/api/cart/") && !path.startsWith("/api/cart/guest/")) {
            return true;
        }

        // 주문 관련
        if (path.startsWith("/api/orders/")) {
            return true;
        }

        // 결제 관련 (게스트, 웹훅 제외)
        if (path.startsWith("/api/payments/") &&
                !path.startsWith("/api/payments/guest/") &&
                !path.startsWith("/api/payments/webhook")) {
            return true;
        }

        // 🔥 Board Service (리뷰) - POST, PUT, DELETE, PATCH 요청은 인증 필요
        if (path.startsWith("/api/board/")) {
            return "POST".equals(method) || "PUT".equals(method) ||
                    "DELETE".equals(method) || "PATCH".equals(method);
        }

        // 🔥 Q&A Service - POST, PUT, DELETE, PATCH 요청은 인증 필요
        if (path.startsWith("/api/qna/")) {
            return "POST".equals(method) || "PUT".equals(method) ||
                    "DELETE".equals(method) || "PATCH".equals(method);
        }

        // 알림 서비스 인증 필요 경로
        if (path.startsWith("/api/notifications/subscriptions/") ||
                path.startsWith("/api/notifications/users/") ||
                path.startsWith("/ws-notifications/")) {
            return true;
        }

        return false;
    }

    // 401 Unauthorized 응답 처리
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