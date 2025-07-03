package org.kosa.notificationservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class UserServiceClientWithFeign {

    @Autowired
    private UserServiceFeignClient userServiceFeignClient;

    /**
     * Feign Client로 사용자 이메일 조회
     */
    public String getUserEmail(String userId) {
        try {
            log.info("Feign으로 사용자 이메일 조회 시도: userId={}", userId);

            UserServiceClient.UserEmailResponse response = userServiceFeignClient.getUserEmail(userId);

            if (response != null && response.isSuccess()) {
                String email = response.getData().getEmail();
                log.info("사용자 이메일 조회 성공: userId={}, email={}***", userId,
                        email.substring(0, Math.min(2, email.length())));
                return email;
            } else {
                log.warn("사용자 이메일 조회 실패: userId={}, response={}", userId, response);
                return null;
            }

        } catch (Exception e) {
            log.error("사용자 이메일 조회 중 오류 발생: userId={}, error={}", userId, e.getMessage(), e);
            return null;
        }
    }
}