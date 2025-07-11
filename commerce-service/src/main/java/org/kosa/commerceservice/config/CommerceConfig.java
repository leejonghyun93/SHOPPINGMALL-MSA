package org.kosa.commerceservice.config;

import com.siot.IamportRestClient.IamportClient;
import feign.RequestInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

@Configuration
@Slf4j
public class CommerceConfig {

    @Value("${iamport.api-key:your_api_key}")
    private String apiKey;

    @Value("${iamport.api-secret:your_api_secret}")
    private String apiSecret;

    @Bean
    public IamportClient iamportClient() {
        log.info("Iamport 클라이언트 초기화 - API Key: {}", apiKey.substring(0, Math.min(10, apiKey.length())) + "...");
        return new IamportClient(apiKey, apiSecret);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            // 공통 헤더 설정
            requestTemplate.header("Content-Type", "application/json");

            // 현재 요청에서 표준 헤더들을 복사
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();

                // Authorization 헤더 전달 (JWT 토큰)
                String authHeader = request.getHeader("Authorization");
                if (authHeader != null) {
                    requestTemplate.header("Authorization", authHeader);
                }

                // User-Agent 헤더 전달
                String userAgent = request.getHeader("User-Agent");
                if (userAgent != null) {
                    requestTemplate.header("User-Agent", userAgent);
                }

                // Accept-Language 헤더 전달
                String acceptLanguage = request.getHeader("Accept-Language");
                if (acceptLanguage != null) {
                    requestTemplate.header("Accept-Language", acceptLanguage);
                }

                // 요청 추적을 위한 헤더들
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