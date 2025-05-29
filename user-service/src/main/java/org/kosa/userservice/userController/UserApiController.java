package org.kosa.userservice.userController;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kosa.userservice.dto.PageDto;
import org.kosa.userservice.dto.PageRequestDto;
import org.kosa.userservice.dto.UserDto;
import org.kosa.userservice.dto.UserResponseDto;
import org.kosa.userservice.entity.User;
import org.kosa.userservice.userService.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserApiController {

    private final UserService userService;

    // 회원 등록
    @PostMapping("/register")
    public ResponseEntity<User> registerUser(@RequestBody User user) {
        User savedUser = userService.saveUser(user);
        return ResponseEntity.ok(savedUser);
    }

    @GetMapping("/{userid}")
    public ResponseEntity<?> getUserDetail(@PathVariable String userid) {
        return userService.getUserDetail(userid)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
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
            if (ex.getMessage().contains("잠겼습니다.")) {
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

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        log.info("헬스체크 요청");
        return ResponseEntity.ok("User Service is running");
    }

    // 회원 정보 수정
    @PutMapping("/edit/{userid}")
    public ResponseEntity<?> updateUser(
            @PathVariable String userid,
            @RequestBody UserDto userDto) {

        if (!userid.equals(userDto.getUserid())) {
            return ResponseEntity.badRequest().body("User ID mismatch");
        }

        try {
            userService.updateUser(userDto);
            return ResponseEntity.ok("User updated successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Update failed");
        }
    }
    @GetMapping("/search")
    public ResponseEntity<?> getUserByNameAndEmail(
            @RequestParam("name") String name,
            @RequestParam("email") String email) {

        return userService.getUserByNameAndEmail(name, email)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("해당하는 사용자를 찾을 수 없습니다."));
    }
    @PutMapping("/{userid}/password")
    public ResponseEntity<?> updatePassword(@PathVariable String userid, @RequestBody String encodedPassword) {
        try {
            userService.updatePassword(userid, encodedPassword);
            return ResponseEntity.ok("비밀번호가 성공적으로 변경되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("비밀번호 변경 중 오류가 발생했습니다.");
        }
    }
}
