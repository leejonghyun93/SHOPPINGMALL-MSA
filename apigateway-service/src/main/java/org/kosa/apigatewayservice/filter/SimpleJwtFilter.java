package org.kosa.apigatewayservice.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
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
import java.util.List;

@Component
@Slf4j
public class SimpleJwtFilter implements WebFilter {

    @Value("${jwt.secret:rrYd2zPDUkx7BUhgDsOTxHCbsBkeTgE/uoARWYSqBjU=}")
    private String jwtSecret;

    private SecretKey getSigningKey() {
        if (jwtSecret.length() < 32) {
            log.warn("JWT secret key가 너무 짧습니다. 최소 32바이트 필요");
            jwtSecret = "rrYd2zPDUkx7BUhgDsOTxHCbsBkeTgE/uoARWYSqBjU=";
        }
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        String method = exchange.getRequest().getMethod().name();

        log.debug("JWT Filter - Path: {}, Method: {}", path, method);

        // CORS OPTIONS 요청은 무조건 통과
        if (HttpMethod.OPTIONS.equals(exchange.getRequest().getMethod())) {
            log.debug(" CORS OPTIONS 요청 - 무조건 통과: {}", path);
            return chain.filter(exchange);
        }

        // 완전 공개 경로는 JWT 검증 스킵
        if (isPublicPath(path, method)) {
            log.debug("공개 경로로 인식, JWT 검증 스킵: {} ({})", path, method);
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        // Authorization 헤더가 없는 경우
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            if (isAuthRequiredPath(path, method)) {
                log.warn("인증 필요 경로인데 토큰 없음: {} ({})", path, method);
                return handleUnauthorized(exchange, "Authorization header missing");
            }
            log.debug(" 토큰 없지만 선택적 인증 경로로 통과: {}", path);
            return chain.filter(exchange);
        }

        String token = authHeader.substring(7);
        log.debug("토큰 추출 완료. 길이: {}", token.length());

        try {
            // JWT 토큰 파싱 및 검증
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            log.debug("JWT 토큰 파싱 성공");

            String userId = extractUserId(claims);
            String role = claims.get("role", String.class);

            if (userId == null || userId.trim().isEmpty()) {
                log.error("사용자 식별자를 찾을 수 없음 - 토큰 거부");
                if (isAuthRequiredPath(path, method)) {
                    return handleUnauthorized(exchange, "사용자 식별자를 찾을 수 없습니다");
                }
                return chain.filter(exchange);
            }

            log.info("JWT 토큰 검증 성공 - UserId: '{}', Role: '{}'", userId, role);

            // Spring Security Context에 인증 정보 설정
            List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                    new SimpleGrantedAuthority("ROLE_" + (role != null ? role : "USER"))
            );

            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(userId, null, authorities);

            log.debug("JWT 인증 성공 - 요청 전달 (토큰 그대로 전달): {}", path);

            // 원본 요청 그대로 전달 (Authorization 헤더 유지)
            return chain.filter(exchange)
                    .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authToken));

        } catch (Exception e) {
            log.error("JWT 파싱 실패: {}", e.getMessage());

            if (isAuthRequiredPath(path, method)) {
                log.error("인증 필요 경로인데 토큰 유효하지 않음: {} ({})", path, method);
                return handleUnauthorized(exchange, "Invalid JWT token: " + e.getMessage());
            }

            log.debug("토큰 무효하지만 선택적 인증 경로로 통과: {}", path);
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

    private boolean isPublicPath(String path, String method) {
        // 개발/운영 환경 공통 공개 경로

        // 인증 관련
        if (path.startsWith("/auth/") || path.equals("/auth")) {
            log.debug("인증 관련 경로로 인식: {}", path);
            return true;
        }

        // 방송 시청자 페이지 (단수형 broadcast)
        if (path.startsWith("/api/broadcast/")) {
            log.debug("방송 시청자 페이지 경로로 인식: {}", path);
            return true;
        }

        // 방송 목록 페이지 (복수형 broadcasts)
        if (path.startsWith("/api/broadcasts/") && "GET".equals(method)) {
            log.debug("방송 목록 페이지 경로로 인식: {}", path);
            return true;
        }

        // 사용자 관련 공개 API
        if (path.startsWith("/api/users/register") ||
                path.startsWith("/api/users/login") ||
                path.startsWith("/api/users/findId") ||
                path.startsWith("/api/users/verify-password") ||
                path.startsWith("/api/users/checkUserId/") ||
                path.startsWith("/api/users/health")) {
            return true;
        }

        // 상품, 카테고리 조회
        if ((path.startsWith("/api/categories/") || path.startsWith("/api/products/")) && "GET".equals(method)) {
            return true;
        }

        // 게스트 장바구니
        if (path.equals("/api/products/guest-cart-details") && "POST".equals(method)) {
            return true;
        }

        // 이미지 및 정적 파일
        if (path.startsWith("/api/images/") ||
                path.startsWith("/images/") ||
                path.startsWith("/upload/") ||
                path.startsWith("/uploads/") ||
                path.startsWith("/static/") ||
                path.startsWith("/resources/") ||
                path.startsWith("/icons/")) {
            return true;
        }

        // 게스트 장바구니 및 결제
        if (path.startsWith("/api/cart/guest/") ||
                path.startsWith("/api/payments/guest/") ||
                path.startsWith("/api/payments/webhook")) {
            return true;
        }

        // 게시판, Q&A 조회
        if ((path.startsWith("/api/board/") || path.startsWith("/api/qna/")) && "GET".equals(method)) {
            return true;
        }

        // 알림 관련 공개 API
        if (path.startsWith("/api/notifications/health") ||
                path.startsWith("/api/notifications/broadcasts/")) {
            return true;
        }

        // 채팅 관련 공개 API
        if (path.startsWith("/api/chat/") && "GET".equals(method)) {
            return true;
        }

        // WebSocket 관련
        if (path.startsWith("/ws/") ||
                path.startsWith("/ws-chat/") ||
                path.startsWith("/websocket/")) {
            return true;
        }

        // 헬스체크 및 모니터링
        if (path.startsWith("/actuator/health/") ||
                path.startsWith("/actuator/prometheus")) {
            return true;
        }

        // Swagger UI 관련
        if (path.startsWith("/swagger-ui/") ||
                path.startsWith("/v3/api-docs/") ||
                path.startsWith("/swagger-resources/") ||
                path.startsWith("/webjars/")) {
            return true;
        }

        return false;
    }

    private boolean isAuthRequiredPath(String path, String method) {
        // 찜하기 API는 인증 필요
        if (path.startsWith("/api/wishlist")) {
            return true;
        }

        // 사용자 프로필 관련
        if (path.startsWith("/api/users/profile") ||
                path.startsWith("/api/users/points") ||
                path.startsWith("/api/users/coupons") ||
                path.startsWith("/api/users/addresses") ||
                path.startsWith("/api/users/withdraw")) {
            return true;
        }

        // 장바구니 (게스트 제외)
        if (path.startsWith("/api/cart") && !path.startsWith("/api/cart/guest/")) {
            return true;
        }

        // 주문 관련
        if (path.startsWith("/api/orders/")) {
            return true;
        }

        // 결제 (게스트 및 웹훅 제외)
        if (path.startsWith("/api/payments/") &&
                !path.startsWith("/api/payments/guest/") &&
                !path.startsWith("/api/payments/webhook")) {
            return true;
        }

        // 상품, 카테고리 관리 (수정 작업)
        if (path.startsWith("/api/products/") || path.startsWith("/api/categories/")) {
            return "POST".equals(method) || "PUT".equals(method) ||
                    "DELETE".equals(method) || "PATCH".equals(method);
        }

        // 게시판, Q&A 관리 (수정 작업)
        if (path.startsWith("/api/board/") || path.startsWith("/api/qna/")) {
            return "POST".equals(method) || "PUT".equals(method) ||
                    "DELETE".equals(method) || "PATCH".equals(method);
        }

        // 방송 관리 (수정 작업)
        if (path.startsWith("/api/broadcasts/")) {
            return "POST".equals(method) || "PUT".equals(method) ||
                    "DELETE".equals(method) || "PATCH".equals(method);
        }

        // 알림 구독 관리
        if (path.startsWith("/api/notifications/subscriptions/") ||
                path.startsWith("/api/notifications/users/") ||
                path.startsWith("/ws-notifications/")) {
            return true;
        }

        return false;
    }

    private Mono<Void> handleUnauthorized(ServerWebExchange exchange, String message) {
        log.warn(" 401 응답 반환: {}", message);

        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().add("Content-Type", "application/json");

        String responseBody = String.format(
                "{\"success\": false, \"message\": \"%s\", \"errorCode\": \"UNAUTHORIZED\", \"timestamp\": \"%s\"}",
                message, java.time.LocalDateTime.now()
        );

        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(responseBody.getBytes()))
        );
    }
}