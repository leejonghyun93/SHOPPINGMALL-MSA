package org.kosa.notificationservice.client;

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

    @Value("${user-service.url:http://localhost:8103}")
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

            // 🔥 API 호출 실패시 fallback으로 테스트 데이터 반환 (개발용)
            return getFallbackUserInfo(userId);
        }
    }

    /**
     * 🔥 API 호출 실패시 fallback 테스트 데이터
     * 실제 운영에서는 제거하고 예외를 던져야 함
     */
    private Map<String, Object> getFallbackUserInfo(String userId) {
        log.warn("User Service API 호출 실패, fallback 데이터 사용: userId={}", userId);

        // 실제 tb_member 테이블의 데이터 구조에 맞춰 반환
        switch (userId) {
            case "qweas":
                return Map.of(
                        "USER_ID", "qweas",
                        "name", "김방송", // tb_member.NAME
                        "email", "kim.broadcast@example.com", // tb_member.EMAIL
                        "phone", "010-1234-5678",
                        "status", "ACTIVE"
                );
            case "admin":
                return Map.of(
                        "USER_ID", "admin",
                        "name", "관리자",
                        "email", "admin@example.com",
                        "status", "ACTIVE"
                );
            case "testuser":
                return Map.of(
                        "USER_ID", "testuser",
                        "name", "테스트유저",
                        "email", "test@example.com",
                        "status", "ACTIVE"
                );
            default:
                return Map.of(
                        "USER_ID", userId,
                        "name", "사용자" + userId,
                        "email", userId + "@example.com",
                        "status", "ACTIVE"
                );
        }
    }

    /**
     * 🔥 실제 운영 환경에서는 이런 메서드로 회원 정보 조회
     * User Service에서 tb_member 테이블 직접 조회하는 API 필요
     */
    /*
    public MemberInfo getMemberInfo(String userId) {
        try {
            String url = userServiceUrl + "/api/members/" + userId;

            ResponseEntity<MemberResponse> response = restTemplate.getForEntity(url, MemberResponse.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                MemberResponse memberResponse = response.getBody();

                return MemberInfo.builder()
                    .userId(memberResponse.getUserId())
                    .name(memberResponse.getName())
                    .email(memberResponse.getEmail())
                    .phone(memberResponse.getPhone())
                    .status(memberResponse.getStatus())
                    .gradeId(memberResponse.getGradeId())
                    .build();
            }

            return null;

        } catch (Exception e) {
            log.error("회원 정보 조회 실패: userId={}", userId, e);
            return null;
        }
    }
    */

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
     * 🔥 실제 회원 정보 DTO (tb_member 테이블 구조에 맞춤)
     */
    public static class MemberInfo {
        private String userId;        // USER_ID
        private String name;          // NAME
        private String email;         // EMAIL
        private String phone;         // PHONE
        private String status;        // STATUS
        private String gradeId;       // GRADE_ID

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

            public MemberInfo build() {
                MemberInfo info = new MemberInfo();
                info.userId = this.userId;
                info.name = this.name;
                info.email = this.email;
                info.phone = this.phone;
                info.status = this.status;
                info.gradeId = this.gradeId;
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
    }
}