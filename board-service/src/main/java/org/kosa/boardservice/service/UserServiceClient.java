package org.kosa.boardservice.service;

import org.kosa.boardservice.dto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "USER-SERVICE") // 반드시 대문자로!
public interface UserServiceClient {

    @GetMapping("/api/users/{userId}")
    UserDto getUserByUserId(@PathVariable("userId") String userId);
}