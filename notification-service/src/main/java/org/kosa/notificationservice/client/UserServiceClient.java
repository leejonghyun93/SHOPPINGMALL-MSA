package org.kosa.notificationservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 간단한 UserService 클라이언트 (Feign 대신 임시)
 * 실제로는 Feign Client나 RestTemplate 사용
 */
@Component
@Slf4j
public class UserServiceClient {

    /**
     * 사용자 이메일 조회 (임시 구현)
     * 실제로는 User Service API 호출
     */
    public String getUserEmail(Long userId) {
        try {
            // TODO: 실제 User Service API 호출로 교체
            // RestTemplate이나 WebClient 사용

            // 임시로 더미 이메일 반환
            return "user" + userId + "@example.com";

        } catch (Exception e) {
            log.error("사용자 이메일 조회 실패: userId={}, error={}", userId, e.getMessage());
            return null;
        }
    }

    /**
     * 실제 구현 예시 (주석 처리)
     */
    /*
    @Autowired
    private RestTemplate restTemplate;

    public String getUserEmail(Long userId) {
        try {
            String url = "http://user-service/api/users/" + userId + "/email";
            return restTemplate.getForObject(url, String.class);
        } catch (Exception e) {
            log.error("사용자 이메일 조회 실패: userId={}", userId, e);
            return null;
        }
    }
    */
}