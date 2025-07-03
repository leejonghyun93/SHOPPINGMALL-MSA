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

                // 🔥 순수 JWT 필터만 사용 (X-헤더 생성하지 않음)
                .addFilterBefore(simpleJwtFilter, SecurityWebFiltersOrder.AUTHENTICATION)

                .authorizeExchange(exchanges ->
                        exchanges
                                // 모든 권한 설정은 기존과 동일
                                .pathMatchers(HttpMethod.GET, "/api/notifications/health").permitAll()
                                .pathMatchers(HttpMethod.GET, "/api/notifications/broadcasts/**").permitAll()
                                .pathMatchers(HttpMethod.POST, "/api/notifications/broadcasts/*/start-notifications").permitAll()
                                .pathMatchers("/actuator/health/**").permitAll()
                                .pathMatchers("/actuator/prometheus").permitAll()

                                .pathMatchers("/auth/**").permitAll()
                                .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                                .pathMatchers(HttpMethod.POST, "/api/users/register").permitAll()
                                .pathMatchers(HttpMethod.POST, "/api/users/login").permitAll()
                                .pathMatchers(HttpMethod.POST, "/api/users/verify-password").permitAll()
                                .pathMatchers(HttpMethod.GET, "/api/users/checkUserId/**").permitAll()
                                .pathMatchers(HttpMethod.GET, "/api/users/health").permitAll()

                                .pathMatchers(HttpMethod.GET, "/api/categories/**").permitAll()
                                .pathMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                                .pathMatchers(HttpMethod.POST, "/api/products/guest-cart-details").permitAll()
                                .pathMatchers(HttpMethod.GET, "/api/images/**").permitAll()
                                .pathMatchers(HttpMethod.GET, "/images/**").permitAll()
                                .pathMatchers(HttpMethod.GET, "/upload/**").permitAll()
                                .pathMatchers(HttpMethod.GET, "/uploads/**").permitAll()
                                .pathMatchers(HttpMethod.GET, "/static/**").permitAll()
                                .pathMatchers(HttpMethod.GET, "/resources/**").permitAll()
                                .pathMatchers(HttpMethod.GET, "/icons/**").permitAll()

                                .pathMatchers(HttpMethod.GET,"/api/users/findId").permitAll()
                                .pathMatchers(HttpMethod.POST, "/auth/findPassword").permitAll()
                                .pathMatchers(HttpMethod.POST, "/auth/verifyResetCode").permitAll()
                                .pathMatchers(HttpMethod.POST, "/auth/resetPassword").permitAll()

                                .pathMatchers(HttpMethod.GET, "/api/broadcasts/**").permitAll()

                                .pathMatchers("/api/cart/guest/**").permitAll()
                                .pathMatchers("/api/payments/guest/**").permitAll()
                                .pathMatchers(HttpMethod.POST, "/api/payments/webhook").permitAll()

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

                                .pathMatchers(HttpMethod.POST, "/api/notifications/subscriptions/**").hasAnyRole("USER", "ADMIN")
                                .pathMatchers(HttpMethod.DELETE, "/api/notifications/subscriptions/**").hasAnyRole("USER", "ADMIN")
                                .pathMatchers(HttpMethod.PUT, "/api/notifications/subscriptions/**").hasAnyRole("USER", "ADMIN")
                                .pathMatchers(HttpMethod.GET, "/api/notifications/users/**").hasAnyRole("USER", "ADMIN")
                                .pathMatchers(HttpMethod.PATCH, "/api/notifications/**").hasAnyRole("USER", "ADMIN")
                                .pathMatchers("/ws-notifications/**").hasAnyRole("USER", "ADMIN")

                                .pathMatchers(HttpMethod.GET, "/api/users/profile").hasAnyRole("USER", "ADMIN")
                                .pathMatchers(HttpMethod.PUT, "/api/users/profile").hasAnyRole("USER", "ADMIN")
                                .pathMatchers(HttpMethod.POST, "/api/users/withdraw").hasAnyRole("USER", "ADMIN")
                                .pathMatchers(HttpMethod.GET, "/api/users/points").hasAnyRole("USER", "ADMIN")
                                .pathMatchers(HttpMethod.GET, "/api/users/coupons").hasAnyRole("USER", "ADMIN")
                                .pathMatchers(HttpMethod.GET, "/api/users/addresses").hasAnyRole("USER", "ADMIN")
                                .pathMatchers(HttpMethod.POST, "/api/users/addresses").hasAnyRole("USER", "ADMIN")
                                .pathMatchers(HttpMethod.PUT, "/api/users/addresses/**").hasAnyRole("USER", "ADMIN")
                                .pathMatchers(HttpMethod.DELETE, "/api/users/addresses/**").hasAnyRole("USER", "ADMIN")

                                .pathMatchers("/api/cart/guest/**").permitAll()
                                .pathMatchers(HttpMethod.POST, "/api/cart").hasAnyRole("USER", "ADMIN")
                                .pathMatchers(HttpMethod.GET, "/api/cart").hasAnyRole("USER", "ADMIN")
                                .pathMatchers(HttpMethod.PUT, "/api/cart/**").hasAnyRole("USER", "ADMIN")
                                .pathMatchers(HttpMethod.DELETE, "/api/cart/**").hasAnyRole("USER", "ADMIN")
                                .pathMatchers("/api/cart/**").hasAnyRole("USER", "ADMIN")

                                .pathMatchers("/api/orders/**").hasAnyRole("USER", "ADMIN")
                                .pathMatchers("/api/payments/**").hasAnyRole("USER", "ADMIN")

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

        // 🔥 X-*** 헤더 노출 제거 - 순수 JWT 방식에서는 불필요
        configuration.setExposedHeaders(List.of(
                "Authorization",
                "Content-Type"
        ));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}