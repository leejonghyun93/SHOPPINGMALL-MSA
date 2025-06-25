package org.kosa.paymentservice.client;

import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

@Configuration
public class FeignClientConfig {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            // 공통 헤더 설정
            requestTemplate.header("Content-Type", "application/json");

            // 현재 요청에서 사용자 정보 헤더들을 복사
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();

                // API Gateway에서 설정한 사용자 정보 헤더들을 그대로 전달
                String userId = request.getHeader("X-User-Id");
                String userRole = request.getHeader("X-User-Role");
                String userName = request.getHeader("X-User-Name");
                String userEmail = request.getHeader("X-User-Email");
                String userPhone = request.getHeader("X-User-Phone");

                if (userId != null) {
                    requestTemplate.header("X-User-Id", userId);
                }
                if (userRole != null) {
                    requestTemplate.header("X-User-Role", userRole);
                }
                if (userName != null) {
                    requestTemplate.header("X-User-Name", userName);
                }
                if (userEmail != null) {
                    requestTemplate.header("X-User-Email", userEmail);
                }
                if (userPhone != null) {
                    requestTemplate.header("X-User-Phone", userPhone);
                }

                // Authorization 헤더도 전달 (있는 경우)
                String authHeader = request.getHeader("Authorization");
                if (authHeader != null) {
                    requestTemplate.header("Authorization", authHeader);
                }
            }
        };
    }

    @Bean
    public ErrorDecoder errorDecoder() {
        return new CustomErrorDecoder();
    }
}