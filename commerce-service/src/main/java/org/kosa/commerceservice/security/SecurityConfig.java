package org.kosa.commerceservice.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CSRF 비활성화 (REST API용)
                .csrf(csrf -> csrf.disable())

                // 세션 관리 설정 (Stateless - JWT 토큰 사용)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // 요청 권한 설정 (통합 서비스 - 모든 요청 허용)
                .authorizeHttpRequests(authz -> authz
                        // 모든 요청 허용 (commerce-service + order-service 통합)
                        .anyRequest().permitAll()
                )

                // HTTP Basic 인증 비활성화
                .httpBasic(httpBasic -> httpBasic.disable())

                // 폼 로그인 비활성화 (JWT 사용)
                .formLogin(formLogin -> formLogin.disable());

        return http.build();
    }
}