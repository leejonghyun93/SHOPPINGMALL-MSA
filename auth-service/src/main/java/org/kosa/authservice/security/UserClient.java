package org.kosa.authservice.security;

import org.kosa.authservice.dto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;


@FeignClient(name = "USER-SERVICE", path = "/api/users")
public interface UserClient {

    // 기존 User Service API 활용
    @GetMapping("/{userId}")
    UserDto getUserByUserId(@PathVariable String userId);


    @GetMapping(value = "/{userId}", produces = "text/plain")
    String getUserByUserIdRaw(@PathVariable String userId);

    // 헬스체크
    @GetMapping("/health")
    String healthCheck();
}