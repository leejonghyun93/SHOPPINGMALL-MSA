package org.kosa.apigatewayservice.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
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
import java.util.Date;
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

        // 🔥 CORS OPTIONS 요청은 무조건 통과
        if (HttpMethod.OPTIONS.equals(request.getMethod())) {
            System.out.println("✅ CORS OPTIONS 요청 - 무조건 통과: " + path);
            return chain.filter(exchange);
        }

        // 🔥 상품/카테고리 GET 요청은 무조건 통과 (인증 불필요)
        if (("GET".equals(method)) &&
                (path.startsWith("/api/categories/") || path.startsWith("/api/products/"))) {
            System.out.println("✅ 상품/카테고리 GET 요청 - 무조건 통과: " + path);
            return chain.filter(exchange);
        }

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

            // 🔥 사용자 ID 추출 로직 수정
            String userId = claims.getSubject();  // 토큰의 subject (username)

            // 🔥 subject가 null인 경우 username 클레임에서 추출
            if (userId == null || userId.trim().isEmpty()) {
                userId = claims.get("username", String.class);
                System.out.println("🔍 subject가 null이므로 username 클레임 사용: " + userId);
            }

            // 🔥 여전히 null인 경우 userId 클레임에서 추출
            if (userId == null || userId.trim().isEmpty()) {
                userId = claims.get("userId", String.class);
                System.out.println("🔍 username도 null이므로 userId 클레임 사용: " + userId);
            }

            String role = claims.get("role", String.class);

            // 🔥 최종 검증
            if (userId == null || userId.trim().isEmpty()) {
                System.err.println("❌ 모든 사용자 식별자가 null - 토큰 거부");
                if (isAuthRequiredPath(path, method)) {
                    return handleUnauthorized(exchange, "사용자 식별자를 찾을 수 없습니다");
                }
                return chain.filter(exchange);
            }

            System.out.println("✅ JWT 토큰 검증 성공 - User: " + userId + ", Role: " + role);

            // 🔥 추가 디버깅 정보
            System.out.println("🔍 토큰 만료시간: " + claims.getExpiration());
            System.out.println("🔍 현재시간: " + new Date());
            System.out.println("🔍 토큰 클레임들: " + claims.keySet());

            // Spring Security Context에만 인증 정보 설정
            List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                    new SimpleGrantedAuthority("ROLE_" + (role != null ? role : "USER"))
            );

            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(userId, null, authorities);

            return chain.filter(exchange)
                    .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authToken));

        } catch (Exception e) {
            // 상세한 에러 로그
            System.err.println("❌ JWT 파싱 실패 - 에러 타입: " + e.getClass().getSimpleName());
            System.err.println("❌ JWT 파싱 실패 - 에러 메시지: " + e.getMessage());

            if (e.getCause() != null) {
                System.err.println("❌ JWT 파싱 실패 - 원인: " + e.getCause().getMessage());
            }

            if (isAuthRequiredPath(path, method)) {
                System.out.println("❌ 인증 필요 경로인데 토큰 유효하지 않음: " + path + " (" + method + ")");
                return handleUnauthorized(exchange, "Invalid JWT token: " + e.getMessage());
            }

            System.out.println("⚠️ 토큰 무효하지만 선택적 인증 경로로 통과: " + path);
            return chain.filter(exchange);
        }
    }

    // 🔥 완전 공개 경로 확인
    private boolean isPublicPath(String path, String method) {
        // 기본 공개 경로들
        if (path.startsWith("/auth/") ||
                path.startsWith("/api/users/register") ||
                path.startsWith("/api/users/login") ||
                path.startsWith("/api/users/findId") ||
                path.startsWith("/api/users/verify-password") ||
                path.startsWith("/api/users/checkUserId/") ||
                path.startsWith("/api/users/health") ||
                path.startsWith("/api/broadcasts/") ||
                path.startsWith("/api/cart/guest/") ||
                path.startsWith("/api/payments/guest/") ||
                path.startsWith("/api/payments/webhook") ||
                path.startsWith("/api/images/") ||
                path.startsWith("/images/") ||
                path.startsWith("/upload/") ||
                path.startsWith("/uploads/") ||
                path.startsWith("/static/") ||
                path.startsWith("/resources/") ||
                path.startsWith("/icons/") ||
                path.equals("/auth/findPassword") ||
                path.equals("/auth/verifyResetCode") ||
                path.equals("/auth/resetPassword") ||
                path.startsWith("/actuator/health/")) {
            return true;
        }

        // 🔥 상품/카테고리 - GET 요청만 공개
        if ((path.startsWith("/api/categories/") || path.startsWith("/api/products/")) && "GET".equals(method)) {
            return true;
        }

        // 🔥 상품 POST 요청 중 게스트 장바구니는 공개
        if (path.equals("/api/products/guest-cart-details") && "POST".equals(method)) {
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

    // 🔥 인증이 필요한 경로 확인
    private boolean isAuthRequiredPath(String path, String method) {
        // 사용자 관련 인증 필요 경로
        if (path.startsWith("/api/users/profile") ||
                path.startsWith("/api/users/points") ||
                path.startsWith("/api/users/coupons") ||
                path.startsWith("/api/users/addresses") ||
                path.startsWith("/api/users/withdraw")) {
            return true;
        }

        // 🔥 장바구니 관련 - 게스트 제외하고 모두 인증 필요
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

        // 🔥 상품/카테고리 - POST, PUT, DELETE, PATCH 요청은 인증 필요
        if (path.startsWith("/api/products/") || path.startsWith("/api/categories/")) {
            return "POST".equals(method) || "PUT".equals(method) ||
                    "DELETE".equals(method) || "PATCH".equals(method);
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