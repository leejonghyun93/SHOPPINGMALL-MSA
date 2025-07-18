package org.kosa.livestreamingservice.client.alarm;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.List;

/**
 * User Service 클라이언트 - Feign Client만 사용
 */
@Component
@Slf4j
@RequiredArgsConstructor
//@ConditionalOnBean(UserServiceFeignClient.class)
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
     * 회원 존재 여부 확인
     */
    public boolean existsUser(String userId) {
        try {
            log.info("사용자 존재 여부 확인: userId={}", userId);

            UserExistsResponse response = userServiceFeignClient.existsUser(userId);

            if (response != null && response.isSuccess()) {
                boolean exists = response.getData().isExists();
                log.info("사용자 존재 여부 확인 완료: userId={}, exists={}", userId, exists);
                return exists;
            } else {
                log.warn("사용자 존재 여부 확인 실패: userId={}, response={}", userId, response);
                return false;
            }

        } catch (Exception e) {
            log.error("사용자 존재 여부 확인 실패: userId={}, error={}", userId, e.getMessage());
            return false;
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

    /**
     * 다중 사용자 정보 조회 (배치 처리용)
     */
    public Map<String, Map<String, Object>> getUserInfoBatch(List<String> userIds) {
        try {
            log.info("배치 사용자 정보 조회: userIds={}", userIds);

            UserInfoBatchResponse response = userServiceFeignClient.getUserInfoBatch(new UserIdListRequest(userIds));

            if (response != null && response.isSuccess()) {
                Map<String, Map<String, Object>> userData = response.getData();
                log.info("배치 사용자 정보 조회 성공: count={}", userData.size());
                return userData;
            } else {
                log.warn("배치 사용자 정보 조회 실패: userIds={}, response={}", userIds, response);
                return Map.of();
            }

        } catch (Exception e) {
            log.error("배치 사용자 정보 조회 실패: userIds={}, error={}", userIds, e.getMessage());
            return Map.of();
        }
    }

    /**
     * 사용자 프로필 이미지 URL 조회
     */
    public String getUserProfileImageUrl(String userId) {
        try {
            Map<String, Object> userInfo = getUserInfo(userId);

            if (userInfo != null) {
                return (String) userInfo.get("profileImg");
            }

            return null;

        } catch (Exception e) {
            log.error("사용자 프로필 이미지 조회 실패: userId={}, error={}", userId, e.getMessage());
            return null;
        }
    }

    /**
     * 사용자 등급 정보 조회
     */
    public String getUserGrade(String userId) {
        try {
            Map<String, Object> userInfo = getUserInfo(userId);

            if (userInfo != null) {
                return (String) userInfo.get("gradeId");
            }

            return null;

        } catch (Exception e) {
            log.error("사용자 등급 조회 실패: userId={}, error={}", userId, e.getMessage());
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

    public static class UserExistsResponse {
        private boolean success;
        private String message;
        private UserExistsData data;

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public UserExistsData getData() { return data; }
        public void setData(UserExistsData data) { this.data = data; }
    }

    public static class UserExistsData {
        private boolean exists;

        public boolean isExists() { return exists; }
        public void setExists(boolean exists) { this.exists = exists; }
    }

    public static class UserInfoBatchResponse {
        private boolean success;
        private String message;
        private Map<String, Map<String, Object>> data;

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public Map<String, Map<String, Object>> getData() { return data; }
        public void setData(Map<String, Map<String, Object>> data) { this.data = data; }
    }

    public static class UserIdListRequest {
        private List<String> userIds;

        public UserIdListRequest() {}
        public UserIdListRequest(List<String> userIds) { this.userIds = userIds; }

        public List<String> getUserIds() { return userIds; }
        public void setUserIds(List<String> userIds) { this.userIds = userIds; }
    }
}