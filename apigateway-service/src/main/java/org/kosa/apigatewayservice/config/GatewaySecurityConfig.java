package org.kosa.apigatewayservice.config;

import org.kosa.apigatewayservice.filter.SimpleJwtFilter;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${FRONTEND_URL:http://localhost:5173}")
    private String frontendUrl;

    @Value("${ADMIN_URL:http://localhost:3000}")
    private String adminUrl;

    @Value("${HOST_IP_ADDRESS:13.209.253.241}")
    private String hostIpAddress;

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
                                //  SWAGGER UI 관련 (인증 무시)
                                // ===========================================
                                .pathMatchers("/swagger-ui/**").permitAll()
                                .pathMatchers("/swagger-ui.html").permitAll()
                                .pathMatchers("/v3/api-docs/**").permitAll()
                                .pathMatchers("/swagger-resources/**").permitAll()
                                .pathMatchers("/webjars/**").permitAll()

                                // 각 서비스별 Swagger UI
                                .pathMatchers("/user-service/swagger-ui/**").permitAll()
                                .pathMatchers("/user-service/v3/api-docs/**").permitAll()
                                .pathMatchers("/live-streaming/swagger-ui/**").permitAll()
                                .pathMatchers("/live-streaming/v3/api-docs/**").permitAll()
                                .pathMatchers("/auth/swagger-ui/**").permitAll()
                                .pathMatchers("/auth/v3/api-docs/**").permitAll()

                                // ===========================================
                                // WEBSOCKET 관련 URL (인증 무시)
                                // ===========================================
                                .pathMatchers("/ws/**").permitAll()
                                .pathMatchers("/ws-chat/**").permitAll()
                                .pathMatchers("/websocket/**").permitAll()

                                // ===========================================
                                // BROADCAST VIEWER 관련 URL (인증 무시)
                                // ===========================================
                                .pathMatchers(HttpMethod.GET, "/api/broadcast/*").permitAll()
                                .pathMatchers(HttpMethod.GET, "/api/broadcast/*/").permitAll()
                                .pathMatchers(HttpMethod.GET, "/api/broadcast/*/products").permitAll()
                                .pathMatchers(HttpMethod.POST, "/api/broadcast/*/view").permitAll()
                                .pathMatchers(HttpMethod.POST, "/api/broadcast/*/like").permitAll()
                                .pathMatchers(HttpMethod.GET, "/api/broadcast/*/status").permitAll()
                                .pathMatchers(HttpMethod.GET, "/api/broadcast/*/chat").permitAll()
                                .pathMatchers(HttpMethod.POST, "/api/broadcast/*/chat").permitAll()

                                // ===========================================
                                // BROADCASTS 관련 URL (인증 무시)
                                // ===========================================
                                .pathMatchers(HttpMethod.GET, "/api/broadcasts/**").permitAll()
                                .pathMatchers(HttpMethod.GET, "/api/broadcasts/search").permitAll()
                                .pathMatchers(HttpMethod.GET, "/api/broadcasts/recommended").permitAll()
                                .pathMatchers(HttpMethod.GET, "/api/broadcasts/stats").permitAll()
                                .pathMatchers(HttpMethod.GET, "/api/broadcasts/health").permitAll()
                                .pathMatchers(HttpMethod.GET, "/api/broadcasts/schedule/**").permitAll()
                                .pathMatchers(HttpMethod.GET, "/api/broadcasts/live").permitAll()
                                .pathMatchers(HttpMethod.GET, "/api/broadcasts/category/**").permitAll()
                                .pathMatchers(HttpMethod.GET, "/api/broadcasts/broadcaster/**").permitAll()
                                .pathMatchers(HttpMethod.GET, "/api/broadcasts/debug/**").permitAll()

                                // ===========================================
                                // CHAT 관련 URL (인증 무시)
                                // ===========================================
                                .pathMatchers(HttpMethod.GET, "/api/chat/**").permitAll()
                                .pathMatchers(HttpMethod.POST, "/api/chat/**").permitAll()
                                .pathMatchers(HttpMethod.GET, "/api/chat/history/**").permitAll()
                                .pathMatchers(HttpMethod.GET, "/api/chat/participants/**").permitAll()
                                .pathMatchers(HttpMethod.POST, "/api/chat/disconnect/**").permitAll()

                                // ===========================================
                                // 기본 시스템 관련
                                // ===========================================
                                .pathMatchers("/actuator/health/**").permitAll()
                                .pathMatchers("/actuator/prometheus").permitAll()
                                .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                                // ===========================================
                                // AUTH 관련
                                // ===========================================
                                .pathMatchers("/auth/**").permitAll()

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
                                .pathMatchers(HttpMethod.POST, "/auth/resetPasswordImmediate").permitAll()

                                // ===========================================
                                // PRODUCT & CATEGORY 관련
                                // ===========================================
                                .pathMatchers(HttpMethod.GET, "/api/categories/**").permitAll()
                                .pathMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                                .pathMatchers(HttpMethod.POST, "/api/products/guest-cart-details").permitAll()

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
                                // 찜하기 API - 인증 필요
                                // ===========================================
                                .pathMatchers(HttpMethod.POST, "/api/wishlist").hasAnyRole("USER", "ADMIN")
                                .pathMatchers(HttpMethod.DELETE, "/api/wishlist/**").hasAnyRole("USER", "ADMIN")
                                .pathMatchers(HttpMethod.GET, "/api/wishlist/check/**").hasAnyRole("USER", "ADMIN")
                                .pathMatchers(HttpMethod.GET, "/api/wishlist").hasAnyRole("USER", "ADMIN")
                                .pathMatchers(HttpMethod.GET, "/api/wishlist/count").hasAnyRole("USER", "ADMIN")
                                .pathMatchers(HttpMethod.DELETE, "/api/wishlist/clear").hasAnyRole("USER", "ADMIN")

                                // ===========================================
                                // USER PROFILE (인증 필요)
                                // ===========================================
                                .pathMatchers(HttpMethod.GET, "/api/users/profile").hasAnyRole("USER", "ADMIN")
                                .pathMatchers(HttpMethod.PUT, "/api/users/profile").hasAnyRole("USER", "ADMIN")
                                .pathMatchers(HttpMethod.POST, "/api/users/withdraw").hasAnyRole("USER", "ADMIN")
                                .pathMatchers(HttpMethod.GET, "/api/users/**").hasAnyRole("USER", "ADMIN")

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
                                // BROADCASTS 관리 API (인증 필요)
                                // ===========================================
                                .pathMatchers(HttpMethod.PATCH, "/api/broadcasts/*/viewers").hasAnyRole("USER", "ADMIN")
                                .pathMatchers(HttpMethod.PATCH, "/api/broadcasts/*/likes").hasAnyRole("USER", "ADMIN")
                                .pathMatchers(HttpMethod.POST, "/api/broadcasts/**").hasAnyRole("USER", "ADMIN")
                                .pathMatchers(HttpMethod.PUT, "/api/broadcasts/**").hasAnyRole("USER", "ADMIN")
                                .pathMatchers(HttpMethod.DELETE, "/api/broadcasts/**").hasAnyRole("USER", "ADMIN")

                                // ===========================================
                                // NOTIFICATIONS (인증 필요)
                                // ===========================================
                                .pathMatchers(HttpMethod.GET, "/api/notifications/health").permitAll()
                                .pathMatchers(HttpMethod.GET, "/api/notifications/broadcasts/**").permitAll()
                                .pathMatchers(HttpMethod.POST, "/api/notifications/broadcasts/*/start-notifications").permitAll()
                                .pathMatchers(HttpMethod.POST, "/api/notifications/subscriptions/**").hasAnyRole("USER", "ADMIN")
                                .pathMatchers(HttpMethod.DELETE, "/api/notifications/subscriptions/**").hasAnyRole("USER", "ADMIN")
                                .pathMatchers(HttpMethod.PUT, "/api/notifications/subscriptions/**").hasAnyRole("USER", "ADMIN")
                                .pathMatchers(HttpMethod.GET, "/api/notifications/users/**").hasAnyRole("USER", "ADMIN")
                                .pathMatchers(HttpMethod.PATCH, "/api/notifications/**").hasAnyRole("USER", "ADMIN")
                                .pathMatchers("/ws-notifications/**").hasAnyRole("USER", "ADMIN")

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

        // 환경에 따른 동적 Origin 설정
        configuration.setAllowedOriginPatterns(List.of(
                "http://localhost:5173",
                "http://localhost:3000",
                "http://127.0.0.1:5173",
                "http://127.0.0.1:3000",
                "http://192.168.*.*:*",        // 로컬 네트워크
                "http://10.*.*.*:*",           // 내부 네트워크
                "http://" + hostIpAddress,     // EC2 IP
                "http://" + hostIpAddress + ":*",  // EC2 IP의 모든 포트
                frontendUrl,                   // 환경변수 프론트엔드 URL
                adminUrl,                      // 환경변수 어드민 URL
                "https://*.shopmall.com",      // 운영 도메인
                "https://shopmall.com"
        ));

        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "PATCH"
        ));

        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        configuration.setExposedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "X-Total-Count"
        ));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}