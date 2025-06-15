package org.kosa.authservice.security;

import org.kosa.authservice.dto.PasswordValidationRequest;
import org.kosa.authservice.dto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "user-service", path = "/api/users")
public interface UserClient {

    @GetMapping("/username/{username}")
    UserDto getUserByUsername(@PathVariable String username);

    @PostMapping("/validate-password")
    boolean validatePassword(@RequestBody PasswordValidationRequest request);
}