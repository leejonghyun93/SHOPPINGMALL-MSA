// GatewaySecurityConfig.java - 수정 버전
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
                                // 🔥 가장 구체적인 경로부터 먼저 (순서 중요!)
                                .pathMatchers(HttpMethod.POST, "/api/users/verify-password").permitAll()
                                .pathMatchers(HttpMethod.POST, "/api/users/register").permitAll()
                                .pathMatchers(HttpMethod.POST, "/api/users/nicknames").permitAll()
                                .pathMatchers(HttpMethod.GET, "/api/users/checkUserId/**").permitAll()
                                .pathMatchers(HttpMethod.GET, "/api/users/health").permitAll()
                                .pathMatchers(HttpMethod.GET, "/api/users/list").permitAll()
                                .pathMatchers(HttpMethod.GET, "/api/users/search").permitAll()
                                .pathMatchers(HttpMethod.GET, "/api/users/profile").permitAll()
                                .pathMatchers(HttpMethod.PUT, "/api/users/profile").permitAll()
                                .pathMatchers(HttpMethod.GET, "/api/users/*").permitAll()
                                .pathMatchers(HttpMethod.PUT, "/api/users/edit/*").permitAll()
                                .pathMatchers(HttpMethod.PUT, "/api/users/*/password").permitAll()
                                .pathMatchers(HttpMethod.PUT, "/api/users/*/password-raw").permitAll()
                                .pathMatchers(HttpMethod.DELETE, "/api/users/delete/*").permitAll()

                                //  완전 공개 경로
                                .pathMatchers("/auth/**").permitAll()

                                //  조회성 API는 인증 불필요
                                .pathMatchers(HttpMethod.GET, "/api/categories/**").permitAll()
                                .pathMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                                .pathMatchers(HttpMethod.POST, "/api/products/guest-cart-details").permitAll()

                                // 🛒 Cart & Order Service
                                .pathMatchers("/api/cart/**").permitAll()
                                .pathMatchers("/api/orders/**").permitAll()
                                .pathMatchers("/api/checkout/**").permitAll()

                                //  정적 리소스
                                .pathMatchers(HttpMethod.GET, "/api/images/**").permitAll()
                                .pathMatchers(HttpMethod.GET, "/images/**").permitAll()
                                .pathMatchers(HttpMethod.GET, "/static/**").permitAll()
                                .pathMatchers(HttpMethod.GET, "/assets/**").permitAll()
                                .pathMatchers("/actuator/health/**").permitAll()

                                //  CORS preflight 요청
                                .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                                .pathMatchers(HttpMethod.POST, "/api/users/withdraw").permitAll()

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

        // ✅ 필요한 헤더들 노출
        configuration.setExposedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "X-User-Id",
                "X-User-Name",
                "X-User-Role",
                "X-Username"
        ));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}