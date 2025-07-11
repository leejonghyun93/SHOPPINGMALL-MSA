package org.kosa.apigatewayservice.config;

import org.kosa.apigatewayservice.filter.SimpleJwtFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebFluxSecurity
public class GatewaySecurityConfig {

    private final SimpleJwtFilter simpleJwtFilter;

    public GatewaySecurityConfig(SimpleJwtFilter simpleJwtFilter) {
        this.simpleJwtFilter = simpleJwtFilter;
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 순수 JWT 필터만 사용 (X-헤더 생성하지 않음)
                .addFilterBefore(simpleJwtFilter, SecurityWebFiltersOrder.AUTHENTICATION)

                .authorizeExchange(exchanges ->
                                exchanges
                                        // ===========================================
                                        // BROADCAST VIEWER 관련 URL (인증 무시) - 🔥 새로 추가!
                                        // ===========================================
                                        .pathMatchers("/ws/**").permitAll()
                                        .pathMatchers("/ws-chat/**").permitAll()
                                        .pathMatchers("/websocket/**").permitAll()

                                        // 방송 시청자 페이지 - 방송 상세 정보 조회
                                        .pathMatchers(HttpMethod.GET, "/api/broadcast/*").permitAll()
                                        .pathMatchers(HttpMethod.GET, "/api/broadcast/*/").permitAll()

                                        // 방송 시청자 페이지 - 상품 목록 조회
                                        .pathMatchers(HttpMethod.GET, "/api/broadcast/*/products").permitAll()

                                        // 방송 시청자 페이지 - 시청자 수 증가 (공개)
                                        .pathMatchers(HttpMethod.POST, "/api/broadcast/*/view").permitAll()

                                        // 방송 시청자 페이지 - 좋아요 (공개)
                                        .pathMatchers(HttpMethod.POST, "/api/broadcast/*/like").permitAll()

                                        // 방송 시청자 페이지 - 상태 확인
                                        .pathMatchers(HttpMethod.GET, "/api/broadcast/*/status").permitAll()

                                        // 방송 시청자 페이지 - 채팅 메시지 조회/전송 (공개)
                                        .pathMatchers(HttpMethod.GET, "/api/broadcast/*/chat").permitAll()
                                        .pathMatchers(HttpMethod.POST, "/api/broadcast/*/chat").permitAll()

                                        // ===========================================
                                        // BROADCASTS 관련 URL (인증 무시) - 기존
                                        // ===========================================

                                        // 방송 조회 관련 (모든 GET 요청 허용)
                                        .pathMatchers(HttpMethod.GET, "/api/broadcasts/**").permitAll()

                                        // 방송 검색 및 추천
                                        .pathMatchers(HttpMethod.GET, "/api/broadcasts/search").permitAll()
                                        .pathMatchers(HttpMethod.GET, "/api/broadcasts/recommended").permitAll()

                                        // 방송 통계 및 헬스체크
                                        .pathMatchers(HttpMethod.GET, "/api/broadcasts/stats").permitAll()
                                        .pathMatchers(HttpMethod.GET, "/api/broadcasts/health").permitAll()

                                        // 방송 일정 조회
                                        .pathMatchers(HttpMethod.GET, "/api/broadcasts/schedule/**").permitAll()

                                        // 라이브 방송 목록
                                        .pathMatchers(HttpMethod.GET, "/api/broadcasts/live").permitAll()

                                        // 카테고리별, 방송자별 방송 목록
                                        .pathMatchers(HttpMethod.GET, "/api/broadcasts/category/**").permitAll()
                                        .pathMatchers(HttpMethod.GET, "/api/broadcasts/broadcaster/**").permitAll()

                                        // 방송 상세 조회
                                        .pathMatchers(HttpMethod.GET, "/api/broadcasts/*/").permitAll()

                                        // 디버그 API
                                        .pathMatchers(HttpMethod.GET, "/api/broadcasts/debug/**").permitAll()

                                        // ===========================================
                                        // 기존 NOTIFICATIONS 관련
                                        // ===========================================
                                        .pathMatchers(HttpMethod.GET, "/api/notifications/health").permitAll()
                                        .pathMatchers(HttpMethod.GET, "/api/notifications/broadcasts/**").permitAll()
                                        .pathMatchers(HttpMethod.POST, "/api/notifications/broadcasts/*/start-notifications").permitAll()

                                        // ===========================================
                                        // 기본 시스템 관련
                                        // ===========================================
                                        .pathMatchers("/actuator/health/**").permitAll()
                                        .pathMatchers("/actuator/prometheus").permitAll()
                                        .pathMatchers(HttpMethod.GET, "/ws/**").permitAll()
                                        .pathMatchers(HttpMethod.GET, "/ws-chat/**").permitAll()  // 추가
                                        .pathMatchers(HttpMethod.GET, "/api/chat/history/**").permitAll()
                                        .pathMatchers(HttpMethod.GET, "/api/chat/**").permitAll()
                                        // ===========================================
                                        // AUTH 관련
                                        // ===========================================
                                        .pathMatchers("/auth/**").permitAll()
                                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                                        // ===========================================
                                        // USER 관련
                                        // ===========================================
                                        .pathMatchers(HttpMethod.POST, "/api/users/register").permitAll()
                                        .pathMatchers(HttpMethod.POST, "/api/users/login").permitAll()
                                        .pathMatchers(HttpMethod.POST, "/api/users/verify-password").permitAll()
                                        .pathMatchers(HttpMethod.GET, "/api/users/checkUserId/**").permitAll()
                                        .pathMatchers(HttpMethod.GET, "/api/users/health").permitAll()
                                        .pathMatchers(HttpMethod.GET, "/api/users/findId").permitAll()
                                        .pathMatchers(HttpMethod.POST, "/auth/findPassword").permitAll()
                                        .pathMatchers(HttpMethod.POST, "/auth/verifyResetCode").permitAll()
                                        .pathMatchers(HttpMethod.POST, "/auth/resetPassword").permitAll()

                                        // ===========================================
                                        // PRODUCT & CATEGORY 관련
                                        // ===========================================
                                        .pathMatchers(HttpMethod.GET, "/api/categories/**").permitAll()
                                        .pathMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                                        .pathMatchers(HttpMethod.POST, "/api/products/guest-cart-details").permitAll()

                                        // 🔥 찜하기 API - 인증 필요로 수정
                                        .pathMatchers(HttpMethod.POST, "/api/wishlist").hasAnyRole("USER", "ADMIN")
                                        .pathMatchers(HttpMethod.DELETE, "/api/wishlist/**").hasAnyRole("USER", "ADMIN")
                                        .pathMatchers(HttpMethod.GET, "/api/wishlist/check/**").hasAnyRole("USER", "ADMIN")
                                        .pathMatchers(HttpMethod.GET, "/api/wishlist").hasAnyRole("USER", "ADMIN")
                                        .pathMatchers(HttpMethod.GET, "/api/wishlist/count").hasAnyRole("USER", "ADMIN")
                                        .pathMatchers(HttpMethod.DELETE, "/api/wishlist/clear").hasAnyRole("USER", "ADMIN")

                                        // ===========================================
                                        // IMAGES & STATIC 관련
                                        // ===========================================
                                        .pathMatchers(HttpMethod.GET, "/api/images/**").permitAll()
                                        .pathMatchers(HttpMethod.GET, "/images/**").permitAll()
                                        .pathMatchers(HttpMethod.GET, "/upload/**").permitAll()
                                        .pathMatchers(HttpMethod.GET, "/uploads/**").permitAll()
                                        .pathMatchers(HttpMethod.GET, "/static/**").permitAll()
                                        .pathMatchers(HttpMethod.GET, "/resources/**").permitAll()
                                        .pathMatchers(HttpMethod.GET, "/icons/**").permitAll()

                                        // ===========================================
                                        // CART & PAYMENTS (GUEST) 관련
                                        // ===========================================
                                        .pathMatchers("/api/cart/guest/**").permitAll()
                                        .pathMatchers("/api/payments/guest/**").permitAll()
                                        .pathMatchers(HttpMethod.POST, "/api/payments/webhook").permitAll()

                                        // ===========================================
                                        // BOARD & QNA (GET 허용, 나머지 인증 필요)
                                        // ===========================================
                                        .pathMatchers(HttpMethod.GET, "/api/board/**").permitAll()
                                        .pathMatchers(HttpMethod.POST, "/api/board/**").hasAnyRole("USER", "ADMIN")
                                        .pathMatchers(HttpMethod.PUT, "/api/board/**").hasAnyRole("USER", "ADMIN")
                                        .pathMatchers(HttpMethod.DELETE, "/api/board/**").hasAnyRole("USER", "ADMIN")
                                        .pathMatchers(HttpMethod.PATCH, "/api/board/**").hasAnyRole("USER", "ADMIN")

                                        .pathMatchers(HttpMethod.GET, "/api/qna/**").permitAll()
                                        .pathMatchers(HttpMethod.POST, "/api/qna").hasAnyRole("USER", "ADMIN")
                                        .pathMatchers(HttpMethod.PUT, "/api/qna/**").hasAnyRole("USER", "ADMIN")
                                        .pathMatchers(HttpMethod.DELETE, "/api/qna/**").hasAnyRole("USER", "ADMIN")
                                        .pathMatchers(HttpMethod.PATCH, "/api/qna/**").hasAnyRole("USER", "ADMIN")

                                        // ===========================================
                                        // NOTIFICATIONS (인증 필요)
                                        // ===========================================
                                        .pathMatchers(HttpMethod.POST, "/api/notifications/subscriptions/**").hasAnyRole("USER", "ADMIN")
                                        .pathMatchers(HttpMethod.DELETE, "/api/notifications/subscriptions/**").hasAnyRole("USER", "ADMIN")
                                        .pathMatchers(HttpMethod.PUT, "/api/notifications/subscriptions/**").hasAnyRole("USER", "ADMIN")
                                        .pathMatchers(HttpMethod.GET, "/api/notifications/users/**").hasAnyRole("USER", "ADMIN")
                                        .pathMatchers(HttpMethod.PATCH, "/api/notifications/**").hasAnyRole("USER", "ADMIN")
                                        .pathMatchers("/ws-notifications/**").hasAnyRole("USER", "ADMIN")

                                        // ===========================================
                                        // BROADCASTS 관리 API (인증 필요)
                                        // ===========================================

                                        // 방송 시청자 수, 좋아요 수 업데이트 (인증 필요)
                                        .pathMatchers(HttpMethod.PATCH, "/api/broadcasts/*/viewers").hasAnyRole("USER", "ADMIN")
                                        .pathMatchers(HttpMethod.PATCH, "/api/broadcasts/*/likes").hasAnyRole("USER", "ADMIN")
                                        // 방송 상세보기 채팅방
                                        .pathMatchers(HttpMethod.GET, "/ws/**").permitAll()
                                        .pathMatchers(HttpMethod.GET, "/api/chat/history/**").permitAll()
                                        .pathMatchers(HttpMethod.GET, "api/chat/**").permitAll()

                                        // 방송 생성, 수정, 삭제 (관리자만)
//                                .pathMatchers(HttpMethod.POST, "/api/broadcasts/**").hasAnyRole("ADMIN")
//                                .pathMatchers(HttpMethod.PUT, "/api/broadcasts/**").hasAnyRole("ADMIN")
//                                .pathMatchers(HttpMethod.DELETE, "/api/broadcasts/**").hasAnyRole("ADMIN")
                                        .pathMatchers(HttpMethod.GET, "/api/stream/**").permitAll()
                                        // GatewaySecurityConfig.java의 authorizeExchange에 추가
                                        .pathMatchers(HttpMethod.GET, "/hls/**").permitAll()
                                        .pathMatchers(HttpMethod.OPTIONS, "/hls/**").permitAll()
// 🚨 게이트웨이 설정에 추가해야 할 라우팅들

// ChatController의 엔드포인트들 중 게이트웨이에 없는 것들:

// 1. ❌ 없음: 참여자 수 조회 API
                                        .pathMatchers(HttpMethod.GET, "/api/chat/participants/**").permitAll()

// 2. ❌ 없음: 수동 연결 해제 API
                                        .pathMatchers(HttpMethod.POST, "/api/chat/disconnect/**").permitAll()

// 3. ❌ 잘못된 경로: 방송 상태 조회 API
// 현재: .pathMatchers(HttpMethod.GET, "/api/broadcast/*/status").permitAll()
// 실제 ChatController: /broadcasts/{broadcastId}/status
                                        .pathMatchers(HttpMethod.GET, "/api/broadcasts/*/status").permitAll()  // 이미 있음 (올바름)

// 🔧 수정된 게이트웨이 설정에 추가할 부분:

// 채팅 관련 API 섹션에 추가:
                                        .pathMatchers(HttpMethod.GET, "/api/chat/participants/**").permitAll()
                                        .pathMatchers(HttpMethod.POST, "/api/chat/disconnect/**").permitAll()

// 또는 더 구체적으로:
                                        .pathMatchers(HttpMethod.GET, "/api/chat/participants/{broadcastId}").permitAll()
                                        .pathMatchers(HttpMethod.POST, "/api/chat/disconnect/{broadcastId}").permitAll()

                                        // ===========================================
                                        // USER PROFILE (인증 필요)
                                        // ===========================================
                                        .pathMatchers(HttpMethod.GET, "/api/users/profile").hasAnyRole("USER", "ADMIN")
                                        .pathMatchers(HttpMethod.PUT, "/api/users/profile").hasAnyRole("USER", "ADMIN")
                                        .pathMatchers(HttpMethod.POST, "/api/users/withdraw").hasAnyRole("USER", "ADMIN")
                                        .pathMatchers(HttpMethod.GET, "/api/users/points").hasAnyRole("USER", "ADMIN")
                                        .pathMatchers(HttpMethod.GET, "/api/users/coupons").hasAnyRole("USER", "ADMIN")
                                        .pathMatchers(HttpMethod.GET, "/api/users/addresses").hasAnyRole("USER", "ADMIN")
                                        .pathMatchers(HttpMethod.POST, "/api/users/addresses").hasAnyRole("USER", "ADMIN")
                                        .pathMatchers(HttpMethod.PUT, "/api/users/addresses/**").hasAnyRole("USER", "ADMIN")
                                        .pathMatchers(HttpMethod.DELETE, "/api/users/addresses/**").hasAnyRole("USER", "ADMIN")

                                        // ===========================================
                                        // CART (인증 필요)
                                        // ===========================================
                                        .pathMatchers(HttpMethod.POST, "/api/cart").hasAnyRole("USER", "ADMIN")
                                        .pathMatchers(HttpMethod.GET, "/api/cart").hasAnyRole("USER", "ADMIN")
                                        .pathMatchers(HttpMethod.PUT, "/api/cart/**").hasAnyRole("USER", "ADMIN")
                                        .pathMatchers(HttpMethod.DELETE, "/api/cart/**").hasAnyRole("USER", "ADMIN")
                                        .pathMatchers("/api/cart/**").hasAnyRole("USER", "ADMIN")

                                        // ===========================================
                                        // ORDERS & PAYMENTS (인증 필요)
                                        // ===========================================
                                        .pathMatchers("/api/orders/**").hasAnyRole("USER", "ADMIN")
                                        .pathMatchers("/api/payments/**").hasAnyRole("USER", "ADMIN")

                                        // ===========================================
                                        // 기타 모든 요청 (인증 필요)
                                        // ===========================================
                                        .anyExchange().authenticated()
                )
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOriginPatterns(List.of(
                "http://localhost:5173",
                "http://localhost:3000",
                "http://127.0.0.1:5173",
                "http://127.0.0.1:3000"
        ));

        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "PATCH"
        ));

        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        configuration.setExposedHeaders(List.of(
                "Authorization",
                "Content-Type"
        ));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}