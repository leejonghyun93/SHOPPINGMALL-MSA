package org.kosa.apigatewayservice.filter;

import io.jsonwebtoken.Claims;
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
public class SimpleJwtFilter extends AbstractGatewayFilterFactory<SimpleJwtFilter.Config> {

    @Value("${jwt.secret-key}")
    private String secretKey;

    public SimpleJwtFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getPath();
            HttpMethod method = request.getMethod();

            log.info("🚀 SimpleJwt Filter - Path: {}, Method: {}", path, method);

            // CORS Preflight 요청은 통과
            if (method == HttpMethod.OPTIONS) {
                log.info("✅ CORS Preflight - 통과: {}", path);
                return chain.filter(exchange);
            }

            // Authorization 헤더 확인
            String authHeader = request.getHeaders().getFirst("Authorization");

            // 🔥 JWT 토큰이 있는 경우 - 파싱 시도
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                try {
                    String token = authHeader.substring(7);

                    SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
                    Claims claims = Jwts.parserBuilder()
                            .setSigningKey(key)
                            .build()
                            .parseClaimsJws(token)
                            .getBody();

                    // 🔥 사용자 정보 추출 (문자열 userId 완벽 지원)
                    String subject = claims.getSubject();
                    String username = claims.get("username", String.class);
                    String name = claims.get("name", String.class);
                    String email = claims.get("email", String.class);
                    String phone = claims.get("phone", String.class);

                    // 🔥 userId 처리: subject를 그대로 사용 (숫자든 문자열이든)
                    String userId = subject != null ? subject : username;
                    String finalUsername = username != null ? username : subject;

                    log.info("✅ JWT 파싱 성공 - Subject: '{}', Username: '{}', Final UserId: '{}'", subject, username, userId);

                    // 헤더에 사용자 정보 추가
                    ServerHttpRequest mutatedRequest = request.mutate()
                            .header("X-User-Id", userId != null ? userId : "")
                            .header("X-Username", finalUsername != null ? finalUsername : "")
                            .header("X-User-Name", name != null ? name : "")
                            .header("X-User-Email", email != null ? email : "")
                            .header("X-User-Phone", phone != null ? phone : "")
                            .header("X-User-Role", "USER")
                            .header("Authorization", authHeader)
                            .build();

                    log.info("🎯 JWT 파싱 성공 - X-User-Id: '{}', X-Username: '{}'", userId, finalUsername);

                    return chain.filter(exchange.mutate().request(mutatedRequest).build());

                } catch (Exception e) {
                    log.warn("⚠️ JWT 파싱 실패하지만 요청 계속 진행: {}", e.getMessage());

                    // 🔥 공개 경로이면 JWT 파싱 실패해도 통과
                    if (isPublicPath(path, method)) {
                        log.info("🔓 공개 경로이므로 JWT 파싱 실패해도 통과: {} [{}]", path, method);
                        return chain.filter(exchange);
                    } else {
                        // 🔥 보호된 경로에서 JWT 파싱 실패하면 401
                        log.error("❌ 보호된 경로에서 JWT 파싱 실패: {} [{}]", path, method);
                        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                        return exchange.getResponse().setComplete();
                    }
                }
            } else {
                // 🔥 JWT 토큰이 없는 경우
                log.info("🔍 JWT 토큰 없음 - 경로 확인: {} [{}]", path, method);

                // 🔥 공개 경로이면 토큰 없어도 통과
                if (isPublicPath(path, method)) {
                    log.info("🔓 공개 경로 - JWT 토큰 없어도 통과: {} [{}]", path, method);
                    return chain.filter(exchange);
                } else {
                    // 🔥 보호된 경로에서 토큰 없으면 401
                    log.warn("❌ 보호된 경로에 JWT 토큰 없음: {} [{}]", path, method);
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                }
            }
        };
    }

    /**
     * 🔥 공개 경로 판단 (JWT 토큰이 없어도 접근 가능한 경로)
     */
    private boolean isPublicPath(String path, HttpMethod method) {
        // 인증/회원가입 관련
        if (path.startsWith("/auth/")) {
            return true;
        }

        // 사용자 서비스 공개 경로
        if ((path.equals("/api/users/register") && method == HttpMethod.POST) ||
                (path.equals("/api/users/verify-password") && method == HttpMethod.POST) ||
                (path.equals("/api/users/profile") && (method == HttpMethod.GET || method == HttpMethod.PUT)) ||
                (path.equals("/api/users/withdraw") && method == HttpMethod.POST) ||
                (path.startsWith("/api/users/checkUserId") && method == HttpMethod.GET) ||
                (path.equals("/api/users/health") && method == HttpMethod.GET) ||
                (path.equals("/api/users/list") && method == HttpMethod.GET)) {
            return true;
        }

        // 상품, 카테고리 조회
        if ((path.startsWith("/api/products/") || path.startsWith("/api/categories/"))
                && method == HttpMethod.GET) {
            return true;
        }

        // guest-cart-details 특별 처리
        if (path.equals("/api/products/guest-cart-details") && method == HttpMethod.POST) {
            return true;
        }

        // 🔥 장바구니 - 모든 메서드 허용 (JWT 있으면 파싱, 없어도 통과)
        if (path.startsWith("/api/cart/")) {
            return true;
        }

        // 🔥 주문 - 모든 메서드 허용 (JWT 있으면 파싱, 없어도 통과)
        if (path.startsWith("/api/orders/") || path.startsWith("/api/checkout/")) {
            return true;
        }

        // 결제 관련 공개 경로
        if (path.startsWith("/api/payments/")) {
            if (method == HttpMethod.GET ||
                    path.equals("/api/payments/webhook") ||
                    path.equals("/api/payments/orders/checkout") ||
                    path.startsWith("/api/payments/guest/")) {
                return true;
            }
        }

        // 정적 리소스
        if ((path.startsWith("/api/images/") || path.startsWith("/images/")) && method == HttpMethod.GET) {
            return true;
        }

        // 헬스체크
        if (path.startsWith("/actuator/health/")) {
            return true;
        }

        return false;
    }

    public static class Config {
        // 설정 클래스 (현재는 비어있음)
    }
}