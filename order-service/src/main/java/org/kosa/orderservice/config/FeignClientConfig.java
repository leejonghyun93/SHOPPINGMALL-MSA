package org.kosa.orderservice.config;

import feign.RequestInterceptor;
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

            // 현재 요청에서 표준 헤더들을 복사
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();

                // 🔥 표준 Authorization 헤더만 전달
                String authHeader = request.getHeader("Authorization");
                if (authHeader != null) {
                    requestTemplate.header("Authorization", authHeader);
                }

                // 🔥 표준 HTTP 헤더들 전달 (필요시)
                String userAgent = request.getHeader("User-Agent");
                if (userAgent != null) {
                    requestTemplate.header("User-Agent", userAgent);
                }

                String acceptLanguage = request.getHeader("Accept-Language");
                if (acceptLanguage != null) {
                    requestTemplate.header("Accept-Language", acceptLanguage);
                }

                // 🔥 요청 추적을 위한 표준 헤더
                String requestId = request.getHeader("Request-ID");
                if (requestId != null) {
                    requestTemplate.header("Request-ID", requestId);
                }

                String traceId = request.getHeader("Trace-ID");
                if (traceId != null) {
                    requestTemplate.header("Trace-ID", traceId);
                }
            }
        };
    }
}