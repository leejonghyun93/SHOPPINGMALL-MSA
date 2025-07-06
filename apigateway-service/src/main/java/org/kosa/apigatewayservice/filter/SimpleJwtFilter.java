package org.kosa.apigatewayservice.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Component
public class SimpleJwtFilter implements WebFilter {

    @Value("${jwt.secret:verySecretKeyThatIsAtLeast32BytesLong1234}")
    private String jwtSecret;

    private SecretKey getSigningKey() {
        if (jwtSecret.length() < 32) {
            System.err.println("⚠️ JWT secret key가 너무 짧습니다. 최소 32바이트 필요");
            jwtSecret = "verySecretKeyThatIsAtLeast32BytesLong1234567890";
        }
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        String method = exchange.getRequest().getMethod().name();

        System.out.println("🔍 JWT Filter - Path: " + path + ", Method: " + method);

        // CORS OPTIONS 요청은 무조건 통과
        if (HttpMethod.OPTIONS.equals(exchange.getRequest().getMethod())) {
            System.out.println("✅ CORS OPTIONS 요청 - 무조건 통과: " + path);
            return chain.filter(exchange);
        }

        // 완전 공개 경로는 JWT 검증 스킵
        if (isPublicPath(path, method)) {
            System.out.println("✅ 공개 경로로 인식, JWT 검증 스킵: " + path + " (" + method + ")");
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        // Authorization 헤더가 없는 경우
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            if (isAuthRequiredPath(path, method)) {
                System.err.println("❌ 인증 필요 경로인데 토큰 없음: " + path + " (" + method + ")");
                return handleUnauthorized(exchange, "Authorization header missing");
            }
            System.out.println("⚠️ 토큰 없지만 선택적 인증 경로로 통과: " + path);
            return chain.filter(exchange);
        }

        String token = authHeader.substring(7);
        System.out.println("🔍 토큰 추출 완료. 길이: " + token.length());

        try {
            // JWT 토큰 파싱 및 검증
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            System.out.println("✅ JWT 토큰 파싱 성공");

            String userId = extractUserId(claims);
            String role = claims.get("role", String.class);

            if (userId == null || userId.trim().isEmpty()) {
                System.err.println("❌ 사용자 식별자를 찾을 수 없음 - 토큰 거부");
                if (isAuthRequiredPath(path, method)) {
                    return handleUnauthorized(exchange, "사용자 식별자를 찾을 수 없습니다");
                }
                return chain.filter(exchange);
            }

            System.out.println("✅ JWT 토큰 검증 성공 - UserId: '" + userId + "', Role: '" + role + "'");

            // 🔥 X-*** 헤더 완전 제거 - 토큰만 그대로 전달
            // 백엔드 서비스들이 각자 토큰을 파싱하여 사용자 정보 추출

            // Spring Security Context에 인증 정보 설정
            List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                    new SimpleGrantedAuthority("ROLE_" + (role != null ? role : "USER"))
            );

            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(userId, null, authorities);

            System.out.println("✅ JWT 인증 성공 - 요청 전달 (X-헤더 없이): " + path);

            // 🔥 원본 요청 그대로 전달 (X-*** 헤더 추가 안함)
            return chain.filter(exchange)
                    .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authToken));

        } catch (Exception e) {
            System.err.println("❌ JWT 파싱 실패: " + e.getMessage());

            if (isAuthRequiredPath(path, method)) {
                System.err.println("❌ 인증 필요 경로인데 토큰 유효하지 않음: " + path + " (" + method + ")");
                return handleUnauthorized(exchange, "Invalid JWT token: " + e.getMessage());
            }

            System.out.println("⚠️ 토큰 무효하지만 선택적 인증 경로로 통과: " + path);
            return chain.filter(exchange);
        }
    }

    private String extractUserId(Claims claims) {
        String userId = claims.getSubject();
        if (isValidUserId(userId)) {
            return userId;
        }

        userId = claims.get("username", String.class);
        if (isValidUserId(userId)) {
            return userId;
        }

        userId = claims.get("userId", String.class);
        if (isValidUserId(userId)) {
            return userId;
        }

        return null;
    }

    private boolean isValidUserId(String userId) {
        return userId != null &&
                !userId.trim().isEmpty() &&
                !"null".equals(userId) &&
                !"undefined".equals(userId);
    }

    // 공개 경로 확인 로직은 기존과 동일
    private boolean isPublicPath(String path, String method) {
        // 🔥 소셜 로그인 콜백 경로 명시적 추가
        if (path.equals("/auth/callback") ||
                path.startsWith("/auth/callback?") ||
                path.startsWith("/auth/social/") ||
                path.startsWith("/auth/")) {
            System.out.println("소셜 로그인 콜백 경로로 인식: " + path);
            return true;
        }

        // 기존 공개 경로들
        if (path.startsWith("/api/users/register") ||
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

        if ((path.startsWith("/api/categories/") || path.startsWith("/api/products/")) && "GET".equals(method)) {
            return true;
        }

        if (path.equals("/api/products/guest-cart-details") && "POST".equals(method)) {
            return true;
        }

        if (path.startsWith("/api/board/") && "GET".equals(method)) {
            return true;
        }

        if (path.startsWith("/api/qna/") && "GET".equals(method)) {
            return true;
        }

        if (path.startsWith("/api/notifications/health") ||
                path.startsWith("/api/notifications/broadcasts/")) {
            return true;
        }

        return false;
    }

    private boolean isAuthRequiredPath(String path, String method) {
        if (path.startsWith("/api/users/profile") ||
                path.startsWith("/api/users/points") ||
                path.startsWith("/api/users/coupons") ||
                path.startsWith("/api/users/addresses") ||
                path.startsWith("/api/users/withdraw")) {
            return true;
        }

        if (path.startsWith("/api/cart") && !path.startsWith("/api/cart/guest/")) {
            return true;
        }

        if (path.startsWith("/api/orders/")) {
            return true;
        }

        if (path.startsWith("/api/payments/") &&
                !path.startsWith("/api/payments/guest/") &&
                !path.startsWith("/api/payments/webhook")) {
            return true;
        }

        if (path.startsWith("/api/products/") || path.startsWith("/api/categories/")) {
            return "POST".equals(method) || "PUT".equals(method) ||
                    "DELETE".equals(method) || "PATCH".equals(method);
        }

        if (path.startsWith("/api/board/")) {
            return "POST".equals(method) || "PUT".equals(method) ||
                    "DELETE".equals(method) || "PATCH".equals(method);
        }

        if (path.startsWith("/api/qna/")) {
            return "POST".equals(method) || "PUT".equals(method) ||
                    "DELETE".equals(method) || "PATCH".equals(method);
        }

        if (path.startsWith("/api/notifications/subscriptions/") ||
                path.startsWith("/api/notifications/users/") ||
                path.startsWith("/ws-notifications/")) {
            return true;
        }

        return false;
    }

    private Mono<Void> handleUnauthorized(ServerWebExchange exchange, String message) {
        System.err.println("🚫 401 응답 반환: " + message);

        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().add("Content-Type", "application/json");

        String responseBody = String.format(
                "{\"success\": false, \"message\": \"%s\", \"errorCode\": \"UNAUTHORIZED\"}",
                message
        );

        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(responseBody.getBytes()))
        );
    }
}