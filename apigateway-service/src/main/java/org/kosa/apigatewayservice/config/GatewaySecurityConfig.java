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

                // 🔥 JWT 필터를 Spring Security 체인에 추가 (올바른 순서 지정)
                .addFilterBefore(simpleJwtFilter, SecurityWebFiltersOrder.AUTHENTICATION)

                .authorizeExchange(exchanges ->
                        exchanges
                                // 🔥 완전 공개 경로 (인증 불필요 + JWT 필터 통과)
                                .pathMatchers("/auth/**").permitAll()
                                .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                                // 🔥 사용자 서비스 - 완전 공개
                                .pathMatchers(HttpMethod.POST, "/api/users/register").permitAll()
                                .pathMatchers(HttpMethod.POST, "/api/users/login").permitAll()
                                .pathMatchers(HttpMethod.POST, "/api/users/verify-password").permitAll()
                                .pathMatchers(HttpMethod.GET, "/api/users/checkUserId/**").permitAll()
                                .pathMatchers(HttpMethod.GET, "/api/users/health").permitAll()

                                // 🔥 상품/카테고리 - 완전 공개 (조회만)
                                .pathMatchers(HttpMethod.GET, "/api/categories/**").permitAll()
                                .pathMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                                .pathMatchers(HttpMethod.POST, "/api/products/guest-cart-details").permitAll()

                                // 🔥 방송 관련 - 완전 공개 (조회만)
                                .pathMatchers(HttpMethod.GET, "/api/broadcasts/**").permitAll()

                                // 🔥 게스트 전용 API - 완전 공개
                                .pathMatchers("/api/cart/guest/**").permitAll()
                                .pathMatchers("/api/payments/guest/**").permitAll()
                                .pathMatchers(HttpMethod.POST, "/api/payments/webhook").permitAll()

                                // 🔥 정적 리소스 - 완전 공개
                                .pathMatchers(HttpMethod.GET, "/api/images/**").permitAll()
                                .pathMatchers(HttpMethod.GET, "/images/**").permitAll()
                                .pathMatchers("/actuator/health/**").permitAll()

                                // 🔥 JWT 인증 필요한 API들 (SimpleJwtFilter에서 처리)
                                // - 마이페이지 관련
                                .pathMatchers(HttpMethod.GET, "/api/users/profile").hasAnyRole("USER", "ADMIN")
                                .pathMatchers(HttpMethod.PUT, "/api/users/profile").hasAnyRole("USER", "ADMIN")
                                .pathMatchers(HttpMethod.POST, "/api/users/withdraw").hasAnyRole("USER", "ADMIN")
                                .pathMatchers(HttpMethod.GET, "/api/users/points").hasAnyRole("USER", "ADMIN")
                                .pathMatchers(HttpMethod.GET, "/api/users/coupons").hasAnyRole("USER", "ADMIN")
                                .pathMatchers(HttpMethod.GET, "/api/users/addresses").hasAnyRole("USER", "ADMIN")
                                .pathMatchers(HttpMethod.POST, "/api/users/addresses").hasAnyRole("USER", "ADMIN")
                                .pathMatchers(HttpMethod.PUT, "/api/users/addresses/**").hasAnyRole("USER", "ADMIN")
                                .pathMatchers(HttpMethod.DELETE, "/api/users/addresses/**").hasAnyRole("USER", "ADMIN")

                                // - 장바구니 (로그인 사용자)
                                .pathMatchers("/api/cart/**").hasAnyRole("USER", "ADMIN")

                                // - 주문 관련
                                .pathMatchers("/api/orders/**").hasAnyRole("USER", "ADMIN")

                                // - 결제 관련 (게스트, 웹훅 제외)
                                .pathMatchers("/api/payments/**").hasAnyRole("USER", "ADMIN")

                                // 🔥 나머지 모든 경로는 인증 필요
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