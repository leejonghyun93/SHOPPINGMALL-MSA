package org.kosa.userservice.userService;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class UserServiceClient {

    private final RestTemplate restTemplate;

    public UserServiceClient(RestTemplateBuilder builder) {
        this.restTemplate = builder.build();
    }

    public Map<String, String> getNicknames(List<String> ids) {
        String url = "http://user-service/api/users/nicknames?ids=" + String.join(",", ids);
        ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
        return response.getBody();
    }
}