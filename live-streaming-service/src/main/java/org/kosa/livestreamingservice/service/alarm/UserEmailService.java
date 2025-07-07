package org.kosa.livestreamingservice.service.alarm;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kosa.livestreamingservice.client.alarm.UserServiceClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserEmailService {

    private final UserServiceClient userServiceClient;

    public String getUserEmail(String userId) {
        log.debug("사용자 이메일 조회: userId={}", userId);

        try {
            // 🔥 실제 UserServiceClient 호출
            String email = userServiceClient.getUserEmail(userId);

            if (email != null && !email.isEmpty()) {
                log.info("UserServiceClient에서 이메일 조회 성공: userId={}, email={}",
                        userId, maskEmail(email));
                return email;
            }

            log.warn("UserServiceClient에서 이메일을 못 가져옴, 기본값 사용: userId={}", userId);

        } catch (Exception e) {
            log.error("UserServiceClient 호출 실패: userId={}, error={}", userId, e.getMessage());
        }

        // 🔥 UserService에서 이메일을 가져오지 못한 경우 null 반환
        log.warn("사용자 이메일을 가져올 수 없습니다: userId={}", userId);
        return null;
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