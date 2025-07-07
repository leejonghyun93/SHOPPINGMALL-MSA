package org.kosa.livestreamingservice.client.alarm;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * User Service 클라이언트 - 실제 tb_member 테이블 정보 조회
 */
@Component
@Slf4j
public class UserServiceClient {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${user-service.url}")
    private String userServiceUrl;

    /**
     * 실제 User Service API에서 사용자 이메일 조회
     * tb_member.EMAIL 조회
     */
    public String getUserEmail(String userId) {
        try {
            log.info("사용자 이메일 조회 시도: userId={}", userId);

            // User Service API 호출
            String url = userServiceUrl + "/api/users/" + userId + "/email";

            // API 응답 받기
            UserEmailResponse response = restTemplate.getForObject(url, UserEmailResponse.class);

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
     * 사용자 기본 정보 조회 (이름 등)
     * tb_member.NAME, EMAIL 등 조회
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getUserInfo(String userId) {
        try {
            String url = userServiceUrl + "/api/users/" + userId;
            log.info("사용자 정보 조회 API 호출: {}", url);

            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();

                // API 응답 구조에 따라 데이터 추출
                if (responseBody.containsKey("data")) {
                    Map<String, Object> userData = (Map<String, Object>) responseBody.get("data");
                    log.info("사용자 정보 조회 성공: userId={}, name={}", userId, userData.get("name"));
                    return userData;
                } else {
                    // 직접 사용자 데이터가 포함된 경우
                    log.info("사용자 정보 조회 성공: userId={}, name={}", userId, responseBody.get("name"));
                    return responseBody;
                }
            }

            log.warn("사용자 정보 조회 실패: userId={}, status={}", userId, response.getStatusCode());
            return null;

        } catch (Exception e) {
            log.error("사용자 정보 조회 중 오류: userId={}, error={}", userId, e.getMessage());

            // 🔥 실제 운영에서는 null 반환 (하드코딩 제거)
            return null;
        }
    }

    /**
     * 🔥 회원 존재 여부 확인
     */
    public boolean existsUser(String userId) {
        try {
            String url = userServiceUrl + "/api/users/" + userId + "/exists";

            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                return (Boolean) responseBody.getOrDefault("exists", false);
            }

            return false;

        } catch (Exception e) {
            log.error("사용자 존재 여부 확인 실패: userId={}, error={}", userId, e.getMessage());
            return false;
        }
    }

    /**
     * 🔥 사용자 이름만 조회
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
     * 🔥 다중 사용자 정보 조회 (배치 처리용)
     */
    public Map<String, Map<String, Object>> getUserInfoBatch(java.util.List<String> userIds) {
        try {
            String url = userServiceUrl + "/api/users/batch";

            Map<String, Object> requestBody = Map.of("userIds", userIds);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, requestBody, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();

                if (responseBody.containsKey("data")) {
                    return (Map<String, Map<String, Object>>) responseBody.get("data");
                }
            }

            log.warn("배치 사용자 정보 조회 실패: userIds={}", userIds);
            return Map.of();

        } catch (Exception e) {
            log.error("배치 사용자 정보 조회 실패: userIds={}, error={}", userIds, e.getMessage());
            return Map.of();
        }
    }

    /**
     * 🔥 사용자 프로필 이미지 URL 조회
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
     * 🔥 사용자 등급 정보 조회
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

    // 응답 DTO 클래스들
    public static class UserEmailResponse {
        private boolean success;
        private String message;
        private UserEmailData data;

        // getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public UserEmailData getData() { return data; }
        public void setData(UserEmailData data) { this.data = data; }
    }

    public static class UserEmailData {
        private String email;

        // getters and setters
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    /**
     * 회원 정보 DTO (tb_member 테이블 구조에 맞춤)
     */
    public static class MemberInfo {
        private String userId;        // USER_ID
        private String name;          // NAME
        private String email;         // EMAIL
        private String phone;         // PHONE
        private String status;        // STATUS
        private String gradeId;       // GRADE_ID
        private String profileImg;    // PROFILE_IMG

        // Builder pattern
        public static MemberInfoBuilder builder() {
            return new MemberInfoBuilder();
        }

        public static class MemberInfoBuilder {
            private String userId;
            private String name;
            private String email;
            private String phone;
            private String status;
            private String gradeId;
            private String profileImg;

            public MemberInfoBuilder userId(String userId) {
                this.userId = userId;
                return this;
            }

            public MemberInfoBuilder name(String name) {
                this.name = name;
                return this;
            }

            public MemberInfoBuilder email(String email) {
                this.email = email;
                return this;
            }

            public MemberInfoBuilder phone(String phone) {
                this.phone = phone;
                return this;
            }

            public MemberInfoBuilder status(String status) {
                this.status = status;
                return this;
            }

            public MemberInfoBuilder gradeId(String gradeId) {
                this.gradeId = gradeId;
                return this;
            }

            public MemberInfoBuilder profileImg(String profileImg) {
                this.profileImg = profileImg;
                return this;
            }

            public MemberInfo build() {
                MemberInfo info = new MemberInfo();
                info.userId = this.userId;
                info.name = this.name;
                info.email = this.email;
                info.phone = this.phone;
                info.status = this.status;
                info.gradeId = this.gradeId;
                info.profileImg = this.profileImg;
                return info;
            }
        }

        // getters
        public String getUserId() { return userId; }
        public String getName() { return name; }
        public String getEmail() { return email; }
        public String getPhone() { return phone; }
        public String getStatus() { return status; }
        public String getGradeId() { return gradeId; }
        public String getProfileImg() { return profileImg; }
    }
}