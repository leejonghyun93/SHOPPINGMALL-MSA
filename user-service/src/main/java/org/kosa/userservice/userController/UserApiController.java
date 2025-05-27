package org.kosa.userservice.userController;

import lombok.RequiredArgsConstructor;
import org.kosa.userservice.dto.PageDto;
import org.kosa.userservice.dto.PageRequestDto;
import org.kosa.userservice.dto.UserResponseDto;
import org.kosa.userservice.entity.User;
import org.kosa.userservice.userService.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/list")
    public PageDto<UserResponseDto> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String searchValue,
            @RequestParam(required = false) String sortBy
    ) {
        PageRequestDto requestDto = new PageRequestDto();
        requestDto.setPage(page);
        requestDto.setSize(size);
        requestDto.setSearchValue(StringUtils.hasText(searchValue) ? searchValue : null);
        requestDto.setSortBy(StringUtils.hasText(sortBy) ? sortBy : "name");

        return userService.list(requestDto);
    }

    @PostMapping("/{userid}/login-fail")
    public ResponseEntity<Void> increaseLoginFail(@PathVariable String userid) {
        try {
            userService.increaseLoginFailCount(userid);
            return ResponseEntity.ok().build();
        } catch (RuntimeException ex) {
            if (ex.getMessage().contains("잠겼")) {
                return ResponseEntity.status(423).body(null); // 423 Locked
            }
            return ResponseEntity.status(400).body(null);
        }
    }

    @PostMapping("/{userid}/login-success")
    public ResponseEntity<Void> resetLoginFail(@PathVariable String userid) {
        userService.resetLoginFailCount(userid);
        return ResponseEntity.ok().build();
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

@PostMapping("/nicknames")
public Map<String, String> getNicknames(@RequestBody List<String> userIds) {
    return userService.getNicknameMapByUserIds(userIds);
}


}
