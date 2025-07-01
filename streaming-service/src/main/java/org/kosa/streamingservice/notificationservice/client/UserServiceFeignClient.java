package org.kosa.streamingservice.notificationservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service", url = "${user-service.url:http://localhost:8103}")
public interface UserServiceFeignClient {

    @GetMapping("/api/users/{userId}/email")
    UserServiceClient.UserEmailResponse getUserEmail(@PathVariable("userId") String userId);
}
