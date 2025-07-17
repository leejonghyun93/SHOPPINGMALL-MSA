package org.kosa.livestreamingservice.client.alarm;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service",
        url = "${external-services.user-service-detail-url:http://user-service:8103}")
public interface UserServiceFeignClient {

    @GetMapping("/api/users/{userId}/email")
    UserServiceClient.UserEmailResponse getUserEmail(@PathVariable("userId") String userId);
}
