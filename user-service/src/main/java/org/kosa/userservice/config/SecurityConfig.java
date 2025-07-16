package org.kosa.userservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf().disable()
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/busrefresh", "/api/users/**").permitAll()
                        .requestMatchers("/api/board/**", "/api/qna/**").permitAll()
                        .requestMatchers("/api/users").permitAll()
                        .requestMatchers("/users/**").permitAll()
                        //  Swagger UI 관련 경로 추가
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/swagger-resources/**", "/webjars/**").permitAll()
                        .anyRequest().authenticated()
                );
        // JWT 필터 제거 - Auth-Service에서 검증 처리

        return http.build();
    }
}