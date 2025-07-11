package org.kosa.userservice.client;

import lombok.extern.slf4j.Slf4j;
import org.kosa.userservice.dto.AuthResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@Slf4j
public class AuthClient {

    private final RestTemplate restTemplate;

    @Value("${auth-service.url:http://localhost:8082}")
    private String authServiceUrl;

    // ✅ LoadBalancer 없는 RestTemplate 사용
    public AuthClient(@Qualifier("restTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Auth-Service에서 토큰 검증
     */
    public AuthResponse validateToken(String token) {
        try {
            log.debug("🔍 Auth-Service 토큰 검증 요청: {}", authServiceUrl);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + token);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<AuthResponse> response = restTemplate.exchange(
                    authServiceUrl + "/auth/validate",
                    HttpMethod.POST,
                    entity,
                    AuthResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                AuthResponse authResponse = response.getBody();
                log.debug("✅ 토큰 검증 성공: userId={}, name={}",
                        authResponse.getUserId(), authResponse.getName());
                return authResponse;
            } else {
                log.warn("⚠️ 토큰 검증 실패: {}", response.getStatusCode());
                return AuthResponse.builder()
                        .success(false)
                        .message("토큰 검증 실패")
                        .build();
            }

        } catch (Exception e) {
            log.error("❌ Auth-Service 호출 실패: {}", e.getMessage());
            return AuthResponse.builder()
                    .success(false)
                    .message("인증 서비스 연결 실패")
                    .build();
        }
    }
}