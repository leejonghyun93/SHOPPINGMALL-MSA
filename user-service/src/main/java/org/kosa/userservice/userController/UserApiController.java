package org.kosa.userservice.userController;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kosa.userservice.JwtUtil;
import org.kosa.userservice.dto.UserDto;
import org.kosa.userservice.entity.Member;
import org.kosa.userservice.userService.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserApiController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    @PostMapping("/register")
    public ResponseEntity<Member> registerUser(@RequestBody Member member) {
        Member savedMember = userService.saveMember(member);
        return ResponseEntity.ok(savedMember);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<?> getUserDetail(@PathVariable String userId) {
        return userService.getMemberDetail(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/delete/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable String userId, HttpServletRequest request) {
        log.info("Delete request for userId: {}", userId);

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or invalid Authorization header");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String token = authHeader.substring(7);

        try {
            if (!jwtUtil.validateToken(token)) {
                log.error("Invalid JWT token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            String tokenUserId = jwtUtil.getUsernameFromToken(token);
            if (tokenUserId == null || !tokenUserId.equals(userId)) {
                log.warn("User ID mismatch - URL: {}, Token: {}", userId, tokenUserId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

        } catch (Exception e) {
            log.error("JWT parsing error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (!userService.isMemberExists(userId)) {
            log.warn("User does not exist: {}", userId);
            return ResponseEntity.notFound().build();
        }

        log.info("Deleting user: {}", userId);
        userService.deleteMember(userId);
        return ResponseEntity.noContent().build();
    }

    // 🔴 깨끗한 checkUserId 메서드 (CORS 헤더 수동 설정 제거)
    @GetMapping("/checkUserId")
    public ResponseEntity<Map<String, Boolean>> checkUserId(@RequestParam String userId) {
        log.info("아이디 중복 확인 요청: {}", userId);

        boolean exists = userService.isMemberExists(userId);
        Map<String, Boolean> response = new HashMap<>();
        response.put("available", !exists);

        log.info("아이디 중복 확인 결과 - userId: {}, available: {}", userId, !exists);

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

    @PutMapping("/edit/{userId}")
    public ResponseEntity<?> updateUser(
            @PathVariable String userId,
            @RequestBody UserDto userDto) {

        if (!userId.equals(userDto.getUserId())) {
            return ResponseEntity.badRequest().body("User ID mismatch");
        }

        try {
            userService.updateMember(userDto);
            return ResponseEntity.ok("User updated successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Update failed");
        }
    }

    @GetMapping("/search")
    public ResponseEntity<?> getUserByNameAndEmail(
            @RequestParam("name") String name,
            @RequestParam("email") String email) {

        return userService.getMemberByNameAndEmail(name, email)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("해당하는 사용자를 찾을 수 없습니다."));
    }

    @PutMapping("/{userId}/password")
    public ResponseEntity<?> updatePassword(@PathVariable String userId, @RequestBody String encodedPassword) {
        try {
            userService.updatePassword(userId, encodedPassword);
            return ResponseEntity.ok("비밀번호가 성공적으로 변경되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("비밀번호 변경 중 오류가 발생했습니다.");
        }
    }

    @PutMapping("/{userId}/password-raw")
    public ResponseEntity<?> updatePasswordRaw(@PathVariable String userId, @RequestBody String rawPassword) {
        try {
            userService.updatePasswordRaw(userId, rawPassword);
            return ResponseEntity.ok("비밀번호가 성공적으로 변경되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("비밀번호 변경 중 오류가 발생했습니다.");
        }
    }
}