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

                                // 🔥 사용자 서비스 공개/반공개 경로
                                .pathMatchers(HttpMethod.POST, "/api/users/register").permitAll()
                                .pathMatchers(HttpMethod.POST, "/api/users/verify-password").permitAll()
                                .pathMatchers(HttpMethod.GET, "/api/users/profile").permitAll()  // 🔥 추가 (SimpleJwtFilter로 토큰 파싱)
                                .pathMatchers(HttpMethod.PUT, "/api/users/profile").permitAll()  // 🔥 추가 (SimpleJwtFilter로 토큰 파싱)
                                .pathMatchers(HttpMethod.POST, "/api/users/withdraw").permitAll()  // 🔥 추가 (SimpleJwtFilter로 토큰 파싱)
                                .pathMatchers(HttpMethod.GET, "/api/users/checkUserId/**").permitAll()
                                .pathMatchers(HttpMethod.GET, "/api/users/health").permitAll()
                                .pathMatchers(HttpMethod.GET, "/api/users/list").permitAll()

                                // 🔥 조회성 API (공개)
                                .pathMatchers(HttpMethod.GET, "/api/categories/**").permitAll()
                                .pathMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                                .pathMatchers(HttpMethod.POST, "/api/products/guest-cart-details").permitAll()

                                // 🔥 게스트 장바구니 + 로그인 사용자 장바구니 모두 허용
                                .pathMatchers("/api/cart/**").permitAll()  // 🔥 모든 장바구니 API 허용 (SimpleJwtFilter에서 토큰 파싱)

                                // 🔥 주문 관련 공개 경로 (모든 주문 API 허용)
                                .pathMatchers("/api/orders/**").permitAll()  // 🔥 모든 주문 API 허용 (SimpleJwtFilter에서 토큰 파싱)
                                .pathMatchers("/api/checkout/**").permitAll()

                                // 🔥 결제 관련 공개 경로
                                .pathMatchers(HttpMethod.GET, "/api/payments/**").permitAll()
                                .pathMatchers(HttpMethod.POST, "/api/payments/webhook").permitAll()
                                .pathMatchers(HttpMethod.POST, "/api/payments/orders/checkout").permitAll()
                                .pathMatchers(HttpMethod.POST, "/api/payments/guest/**").permitAll()

                                // 🔥 정적 리소스
                                .pathMatchers(HttpMethod.GET, "/api/images/**").permitAll()
                                .pathMatchers(HttpMethod.GET, "/images/**").permitAll()
                                .pathMatchers("/actuator/health/**").permitAll()

                                // 🔥 인증이 필요한 경로들은 JWT 필터에서 처리
                                // SimpleJwtFilter가 토큰을 파싱하고 X-User-Id 헤더를 추가
                                // JwtAuthorizationFilter가 실제 인증을 강제
                                .anyExchange().authenticated()
                )
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOriginPatterns(List.of(
                "http://localhost:5173",
                "http://localhost:3000"
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