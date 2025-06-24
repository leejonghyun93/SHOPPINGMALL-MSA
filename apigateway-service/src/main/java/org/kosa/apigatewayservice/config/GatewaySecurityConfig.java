package org.kosa.apigatewayservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
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

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeExchange(exchanges ->
                        exchanges
                                // 🔥 완전 공개 경로 (인증 불필요)
                                .pathMatchers("/auth/**").permitAll()
                                .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                                // 🔥 사용자 서비스 - 진짜 공개 경로만
                                .pathMatchers(HttpMethod.POST, "/api/users/register").permitAll()
                                .pathMatchers(HttpMethod.POST, "/api/users/verify-password").permitAll()
                                .pathMatchers(HttpMethod.GET, "/api/users/checkUserId/**").permitAll()
                                .pathMatchers(HttpMethod.GET, "/api/users/health").permitAll()

                                // 🔥 상품/카테고리 - 진짜 공개 (조회만)
                                .pathMatchers(HttpMethod.GET, "/api/categories/**").permitAll()
                                .pathMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                                .pathMatchers(HttpMethod.POST, "/api/products/guest-cart-details").permitAll()

                                // 🔥 방송 관련 - 진짜 공개 (조회만)
                                .pathMatchers(HttpMethod.GET, "/api/broadcasts/**").permitAll()

                                // 🔥 게스트 장바구니만 공개
                                .pathMatchers(HttpMethod.GET, "/api/cart/guest/**").permitAll()
                                .pathMatchers(HttpMethod.POST, "/api/cart/guest/**").permitAll()

                                // 🔥 결제 관련 - 웹훅과 게스트 결제만 공개
                                .pathMatchers(HttpMethod.POST, "/api/payments/webhook").permitAll()
                                .pathMatchers(HttpMethod.POST, "/api/payments/guest/**").permitAll()

                                // 🔥 정적 리소스
                                .pathMatchers(HttpMethod.GET, "/api/images/**").permitAll()
                                .pathMatchers(HttpMethod.GET, "/images/**").permitAll()
                                .pathMatchers("/actuator/health/**").permitAll()

                                // 🔥 나머지 모든 경로는 JWT 인증 필요
                                // - /api/users/** (profile, points, coupons, addresses 등)
                                // - /api/orders/** (주문 관련 모든 API)
                                // - /api/payments/** (웹훅, 게스트 제외한 모든 결제 API)
                                // - /api/cart/** (게스트 제외한 모든 장바구니 API)
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
                "Content-Type",
                "X-User-Id",
                "X-User-Name",
                "X-User-Role",
                "X-Username",
                "X-User-Email",
                "X-User-Phone"
        ));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}