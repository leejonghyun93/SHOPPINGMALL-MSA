package org.kosa.userservice.userController;

import lombok.RequiredArgsConstructor;
import org.kosa.userservice.entity.User;
import org.kosa.userservice.userService.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserApiController {

    private final UserService userService;

    // 회원 등록
    @PostMapping
    public ResponseEntity<User> registerUser(@RequestBody User user) {
        User savedUser = userService.saveUser(user);
        return ResponseEntity.ok(savedUser);
    }

    // 모든 회원 조회
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    // 단일 회원 조회
    @GetMapping("/{userid}")
    public ResponseEntity<User> getUser(@PathVariable String userid) {
        return userService.getUserById(userid)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 회원 삭제
    @DeleteMapping("/{userid}")
    public ResponseEntity<Void> deleteUser(@PathVariable String userid) {
        if (!userService.isUserExists(userid)) {
            return ResponseEntity.notFound().build();
        }
        userService.deleteUser(userid);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/checkUserid")
    public ResponseEntity<Map<String, Boolean>> checkUserid(@RequestParam String userid) {
        boolean exists = userService.isUserExists(userid);
        Map<String, Boolean> response = new HashMap<>();
        response.put("available", !exists); // 아이디가 없으면 사용 가능(available=true)
        return ResponseEntity.ok(response);
    }
}
