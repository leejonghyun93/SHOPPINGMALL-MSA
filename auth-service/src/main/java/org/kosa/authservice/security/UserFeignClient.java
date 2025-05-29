package org.kosa.authservice.security;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/users/search")
    UserDto getUserByNameAndEmail(@RequestParam("name") String name, @RequestParam("email") String email);

    @PutMapping("/users/{userid}/password")
    void updatePassword(@PathVariable("userid") String userid, @RequestBody String encodedPassword);

}