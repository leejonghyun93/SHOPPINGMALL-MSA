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
                                // 🔥 명시적으로 공개 경로 정의
                                .pathMatchers(
                                        "/auth/**",
                                        "/api/users/register",
                                        "/api/users/checkUserId/**",
                                        "/api/users/health",
                                        "/api/users/list",
                                        "/api/users/verify-password",
                                        "/api/users/profile"
                                ).permitAll()
                                // 🔥 카테고리와 상품 서비스는 GET 요청만 허용
                                .pathMatchers(HttpMethod.GET, "/api/categories/**").permitAll()
                                .pathMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                                // 🔥 이미지 서비스 추가!
                                .pathMatchers(HttpMethod.GET, "/api/images/**").permitAll()
                                .pathMatchers(HttpMethod.GET, "/images/**").permitAll()
                                .pathMatchers(HttpMethod.GET, "/static/**").permitAll()
                                .pathMatchers(HttpMethod.GET, "/assets/**").permitAll()
                                // 🔥 OPTIONS 요청은 모두 허용 (CORS preflight)
                                .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                                // 나머지는 인증 필요
                                .anyExchange().authenticated()
                )
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 🔥 allowedOrigins 대신 allowedOriginPatterns 사용 (더 안전)
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

        // 🔥 노출할 헤더 명시적 설정
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