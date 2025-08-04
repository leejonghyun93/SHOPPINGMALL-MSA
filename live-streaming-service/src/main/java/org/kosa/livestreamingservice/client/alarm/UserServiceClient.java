package org.kosa.livestreamingservice.client.alarm;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * User Service 클라이언트 - Feign Client만 사용
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class UserServiceClient {

    private final UserServiceFeignClient userServiceFeignClient;

    /**
     * Feign Client로 사용자 이메일 조회
     * tb_member.EMAIL 조회
     */
    public String getUserEmail(String userId) {
        try {
            log.info("사용자 이메일 조회 시도: userId={}", userId);

            UserEmailResponse response = userServiceFeignClient.getUserEmail(userId);

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

    /**
     * Feign Client로 사용자 기본 정보 조회 (이름 등)
     * tb_member.NAME, EMAIL 등 조회
     */
    public Map<String, Object> getUserInfo(String userId) {
        try {
            log.info("사용자 정보 조회 시도: userId={}", userId);

            UserInfoResponse response = userServiceFeignClient.getUserInfo(userId);

            if (response != null && response.isSuccess()) {
                Map<String, Object> userData = response.getData();
                log.info("사용자 정보 조회 성공: userId={}, name={}", userId, userData.get("name"));
                return userData;
            } else {
                log.warn("사용자 정보 조회 실패: userId={}, response={}", userId, response);
                return null;
            }

        } catch (Exception e) {
            log.error("사용자 정보 조회 중 오류: userId={}, error={}", userId, e.getMessage());
            return null;
        }
    }

    /**
     * 사용자 이름만 조회
     */
    public String getUserName(String userId) {
        try {
            Map<String, Object> userInfo = getUserInfo(userId);

            if (userInfo != null) {
                String name = (String) userInfo.get("name");
                if (name != null && !name.trim().isEmpty()) {
                    return name;
                }
            }

            log.warn("사용자 이름을 찾을 수 없습니다: userId={}", userId);
            return null;

        } catch (Exception e) {
            log.error("사용자 이름 조회 실패: userId={}, error={}", userId, e.getMessage());
            return null;
        }
    }

    // DTO 클래스들
    public static class UserEmailResponse {
        private boolean success;
        private String message;
        private UserEmailData data;

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public UserEmailData getData() { return data; }
        public void setData(UserEmailData data) { this.data = data; }
    }

    public static class UserEmailData {
        private String email;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    public static class UserInfoResponse {
        private boolean success;
        private String message;
        private Map<String, Object> data;

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public Map<String, Object> getData() { return data; }
        public void setData(Map<String, Object> data) { this.data = data; }
    }
}