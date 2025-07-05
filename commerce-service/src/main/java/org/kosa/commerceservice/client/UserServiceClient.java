package org.kosa.commerceservice.client;


import org.kosa.commerceservice.dto.user.UserValidationResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service", url = "http://localhost:8103")
public interface UserServiceClient {

    @GetMapping("/api/users/{userId}/validate")
    UserValidationResponse validateUser(@PathVariable String userId);
}