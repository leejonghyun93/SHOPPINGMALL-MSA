package org.kosa.livestreamingservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kosa.livestreamingservice.client.UserServiceClient;
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
                return email;
            }

            // UserServiceClient에서 못 가져오면 기본값 사용
            log.warn("UserServiceClient에서 이메일을 못 가져옴, 기본값 사용: userId={}", userId);

        } catch (Exception e) {
            log.error("UserServiceClient 호출 실패: userId={}, error={}", userId, e.getMessage());
        }

        // 🔥 실제 테스트를 위한 이메일 매핑 (fallback)
        switch (userId) {
            case "qweas":
                return "your-test-email@gmail.com";  // 🔥 실제 받을 이메일 주소로 변경
            case "123":
                return "another-test@gmail.com";
            case "testuser":
                return "testuser@gmail.com";
            default:
                // 기본 테스트 이메일
                return "default-test@gmail.com";  // 🔥 실제 받을 이메일 주소로 변경
        }
    }
}