package org.kosa.authservice.security;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "user-service", path = "/api/users")
public interface UserFeignClient {

    @GetMapping("/{userid}")
    UserDto getUserByUserId(@PathVariable("userid") String userid);

    @PostMapping("/{userid}/login-fail")
    void increaseLoginFailCount(@PathVariable("userid") String userid);

    @PostMapping("/{userid}/login-success")
    void resetLoginFailCount(@PathVariable("userid") String userid);

    @GetMapping("/users")
    UserDto getUser(@RequestParam("username") String username);


}