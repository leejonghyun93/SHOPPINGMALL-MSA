package org.kosa.boardservice.service;

import org.kosa.boardservice.dto.UserDto;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class UserRestClient {

    private final RestTemplate restTemplate;

    public UserRestClient(RestTemplateBuilder builder) {
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(10))
                .build();
    }

    public UserDto getUserByUserId(String userId) {
        try {
            String url = "http://user-service/api/users/" + userId;
            System.out.println("유저 서비스 호출 URL: " + url);

            ResponseEntity<UserDto> response = restTemplate.getForEntity(url, UserDto.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                System.out.println("유저 정보 조회 성공: " + response.getBody().getName());
                return response.getBody();
            } else {
                System.err.println("유저 정보 조회 실패 - 응답 상태: " + response.getStatusCode());
                throw new RuntimeException("유저 정보를 찾을 수 없습니다");
            }

        } catch (RestClientException e) {
            System.err.println("유저 서비스 호출 실패: " + e.getMessage());
            throw new RuntimeException("유저 서비스 연결 실패", e);
        }
    }
}