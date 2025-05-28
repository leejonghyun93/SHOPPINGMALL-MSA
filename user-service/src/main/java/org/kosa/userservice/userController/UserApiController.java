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
    @PostMapping
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
    @PutMapping("/{userid}")
    public ResponseEntity<?> updateUser(
            @PathVariable String userid,
            @RequestBody User updatedUser,
            HttpServletRequest request
    ) {
        // 권한 체크는 실제로 JWT 또는 SecurityContext에서 구현해야 합니다.

        // 1. 기존 사용자 엔티티 조회 (Optional<User>)
        Optional<User> existingUserOpt = userService.findUserEntityByUserid(userid);
        if (existingUserOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User existingUser = existingUserOpt.get();

        // 2. 변경 가능한 필드만 안전하게 업데이트
        if (StringUtils.hasText(updatedUser.getEmail())) {
            existingUser.setEmail(updatedUser.getEmail());
        }
        if (StringUtils.hasText(updatedUser.getNickname())) {
            existingUser.setNickname(updatedUser.getNickname());
        }
        if (StringUtils.hasText(updatedUser.getPhone())) {
            existingUser.setPhone(updatedUser.getPhone());
        }
        if (StringUtils.hasText(updatedUser.getAddress())) {
            existingUser.setAddress(updatedUser.getAddress());
        }
        if (StringUtils.hasText(updatedUser.getDetailAddress())) {
            existingUser.setDetailAddress(updatedUser.getDetailAddress());
        }

        // 비밀번호 변경 시 암호화 처리 (빈 값이나 null이면 변경 안함)
        if (StringUtils.hasText(updatedUser.getPasswd())) {
            existingUser.setPasswd(userService.encodePassword(updatedUser.getPasswd()));
        }

        // 3. 저장
        User savedUser = userService.saveUser(existingUser);

        // 4. 저장된 사용자 DTO 변환 후 응답
        UserResponseDto responseDto = userService.toDto(savedUser);

        return ResponseEntity.ok(responseDto);
    }
}
