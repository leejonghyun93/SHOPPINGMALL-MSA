// GatewaySecurityConfig.java - Cart Service 부분 수정
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
                                .pathMatchers(
                                        "/auth/**",
                                        "/api/users/register",
                                        "/api/users/checkUserId/**",
                                        "/api/users/health",
                                        "/api/users/list",
                                        "/api/users/verify-password"
                                ).permitAll()

                                // 🔥 조회성 API는 인증 불필요
                                .pathMatchers(HttpMethod.GET, "/api/categories/**").permitAll()
                                .pathMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                                .pathMatchers(HttpMethod.GET, "/api/users/profile").permitAll()

                                // 🛒 Cart Service - 비회원도 사용 가능
                                .pathMatchers("/api/cart/**").permitAll()
                                .pathMatchers("/api/cart/guest/**").permitAll()

                                // 🛒 Order Service - 조회는 공개, 주문은 인증 필요
                                .pathMatchers(HttpMethod.GET, "/api/orders/checkout/**").permitAll()
                                .pathMatchers(HttpMethod.GET, "/api/checkout/**").permitAll()
                                // 실제 주문 생성/수정/삭제는 인증 필요 (나중에 처리)

                                // 🔥 정적 리소스
                                .pathMatchers(HttpMethod.GET, "/api/images/**").permitAll()
                                .pathMatchers(HttpMethod.GET, "/images/**").permitAll()
                                .pathMatchers(HttpMethod.GET, "/static/**").permitAll()
                                .pathMatchers(HttpMethod.GET, "/assets/**").permitAll()
                                .pathMatchers("/actuator/health/**").permitAll()

                                // 🔥 CORS preflight 요청
                                .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                                // 나머지는 인증 필요
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
                "http://localhost:*"
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
                "X-User-Role"
        ));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}