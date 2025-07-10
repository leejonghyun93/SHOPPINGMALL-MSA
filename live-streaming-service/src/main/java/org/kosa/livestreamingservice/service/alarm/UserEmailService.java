package org.kosa.livestreamingservice.service.alarm;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kosa.livestreamingservice.client.alarm.UserServiceClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserEmailService {

    private final UserServiceClient userServiceClient;

    @CircuitBreaker(name = "user-service", fallbackMethod = "getUserEmailFallback")
    @Retry(name = "user-service")
    public String getUserEmail(String userId) {
        log.debug("사용자 이메일 조회: userId={}", userId);

        try {
            //  기존 UserServiceClient 그대로 사용
            String email = userServiceClient.getUserEmail(userId);

            if (email != null && !email.isEmpty()) {
                log.info("UserServiceClient에서 이메일 조회 성공: userId={}, email={}",
                        userId, maskEmail(email));
                return email;
            }

            log.warn("UserServiceClient에서 이메일을 못 가져옴: userId={}", userId);
            return null;

        } catch (Exception e) {
            log.error("UserServiceClient 호출 실패: userId={}, error={}", userId, e.getMessage());
            throw e;  // Circuit Breaker가 처리
        }
    }

    public String getUserEmailFallback(String userId, Exception ex) {
        log.warn(" User Service 장애 - 이메일 조회 실패: userId={}, error={}",
                userId, ex.getMessage());
        return null;  // 이메일 발송 스킵
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return email;
        String[] parts = email.split("@");
        String localPart = parts[0];
        String domain = parts[1];
        if (localPart.length() <= 2) return email;
        return localPart.substring(0, 2) + "***@" + domain;
    }
}