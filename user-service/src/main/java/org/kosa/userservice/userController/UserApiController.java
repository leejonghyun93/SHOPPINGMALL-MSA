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

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserApiController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    @GetMapping("/findId")
    public ResponseEntity<?> findId(
            @RequestParam("name") String name,
            @RequestParam("email") String email) {

        log.info("아이디 찾기 요청 - name: {}, email: {}***", name,
                email.length() > 3 ? email.substring(0, 3) : email);

        try {
            // 입력 검증
            if (name == null || name.trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "이름을 입력해주세요.");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            if (email == null || email.trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "이메일을 입력해주세요.");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // 이름과 이메일로 사용자 찾기
            Optional<UserDto> userOpt = userService.getMemberByNameAndEmail(name.trim(), email.trim());

            if (userOpt.isPresent()) {
                UserDto user = userOpt.get();

                log.info("아이디 찾기 성공 - name: {}, email: {}***, userId: {}",
                        name, email.substring(0, Math.min(3, email.length())), user.getUserId());

                // 성공 응답
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "아이디 찾기 성공");
                response.put("userId", user.getUserId());
                response.put("timestamp", LocalDateTime.now());

                return ResponseEntity.ok(response);

            } else {
                log.warn("아이디 찾기 실패 - 일치하는 사용자 없음: name={}, email={}***",
                        name, email.length() > 3 ? email.substring(0, 3) : email);

                // 404 응답
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "입력하신 정보와 일치하는 계정을 찾을 수 없습니다.");
                response.put("timestamp", LocalDateTime.now());

                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

        } catch (Exception e) {
            log.error("아이디 찾기 중 오류 발생 - name: {}, email: {}***, error: {}",
                    name, email.length() > 3 ? email.substring(0, 3) : email, e.getMessage(), e);

            // 500 응답
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

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

    // 깨끗한 checkUserId 메서드 (CORS 헤더 수동 설정 제거)
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

    @PostMapping("/verify-password")
    public ResponseEntity<?> verifyPassword(
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest) {

        try {
            String password = request.get("password");
            if (password == null || password.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "비밀번호를 입력해주세요."));
            }

            // JWT 토큰 추출
            String authHeader = httpRequest.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "인증이 필요합니다."));
            }

            String token = authHeader.substring(7);
            if (!jwtUtil.validateToken(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "유효하지 않은 토큰입니다."));
            }

            String userId = jwtUtil.getUsernameFromToken(token);
            log.info("비밀번호 검증 요청 - userId: {}", userId);

            // 실제 DB에서 사용자 조회
            Optional<UserDto> userOpt = userService.getMemberDetail(userId);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "사용자를 찾을 수 없습니다."));
            }

            UserDto user = userOpt.get();

            // 실제 비밀번호 검증 (DB 암호화된 비밀번호와 비교)
            boolean isValid = userService.matchesPassword(password, user.getPassword());

            log.info("비밀번호 검증 결과 - userId: {}, 결과: {}", userId, isValid ? "성공" : "실패");

            if (isValid) {
                return ResponseEntity.ok(Map.of("message", "비밀번호가 확인되었습니다."));
            } else {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "비밀번호가 일치하지 않습니다."));
            }

        } catch (Exception e) {
            log.error("비밀번호 검증 중 오류: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "비밀번호 확인 중 오류가 발생했습니다."));
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getUserProfile(HttpServletRequest httpRequest) {
        try {
            // JWT 토큰 추출
            String authHeader = httpRequest.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "인증이 필요합니다."));
            }

            String token = authHeader.substring(7);
            if (!jwtUtil.validateToken(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "유효하지 않은 토큰입니다."));
            }

            String userId = jwtUtil.getUsernameFromToken(token);
            log.info("프로필 조회 요청 - userId: {}", userId);

            // 실제 DB에서 사용자 상세 정보 조회
            Optional<UserDto> userOpt = userService.getMemberDetail(userId);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "사용자를 찾을 수 없습니다."));
            }

            UserDto user = userOpt.get();

            // 응답용 데이터 (비밀번호 제외)
            Map<String, Object> response = new HashMap<>();
            response.put("userId", user.getUserId());
            response.put("name", user.getName());
            response.put("email", user.getEmail());
            response.put("phone", user.getPhone());
            response.put("birthDate", user.getBirthDate());
            response.put("address", user.getAddress());
            response.put("zipcode", user.getZipcode());
            response.put("gender", user.getGender());

            log.info("프로필 조회 성공 - userId: {}", userId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("프로필 조회 중 오류: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "프로필 정보를 불러올 수 없습니다."));
        }
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateUserProfile(
            @RequestBody UserDto userDto,
            HttpServletRequest httpRequest) {

        try {
            // JWT 토큰 추출
            String authHeader = httpRequest.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "인증이 필요합니다."));
            }

            String token = authHeader.substring(7);
            if (!jwtUtil.validateToken(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "유효하지 않은 토큰입니다."));
            }

            String userId = jwtUtil.getUsernameFromToken(token);
            log.info("프로필 수정 요청 - userId: {}", userId);

            // 필수 필드 검증
            if (userDto.getName() == null || userDto.getName().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "이름은 필수 항목입니다."));
            }

            if (userDto.getEmail() == null || userDto.getEmail().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "이메일은 필수 항목입니다."));
            }

            // UserDto에 userId 설정
            userDto.setUserId(userId);

            // 실제 DB 업데이트 (기존 updateMember 메서드 사용)
            userService.updateMember(userDto);

            log.info("프로필 수정 완료 - userId: {}", userId);
            return ResponseEntity.ok(Map.of("message", "프로필이 성공적으로 수정되었습니다."));

        } catch (Exception e) {
            log.error("프로필 수정 중 오류: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "프로필 수정 중 오류가 발생했습니다."));
        }
    }

    @GetMapping("/{userId}/email")
    public ResponseEntity<?> getUserEmail(@PathVariable String userId) {
        try {
            log.info("사용자 이메일 조회 요청: userId={}", userId);

            // UserService에서 이메일 조회
            String email = userService.getUserEmailByUserId(userId);

            if (email != null && !email.isEmpty()) {
                log.info("사용자 이메일 조회 성공: userId={}, email={}***", userId,
                        email.substring(0, Math.min(2, email.length())));

                // 성공 응답
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "사용자 이메일 조회 성공");

                Map<String, String> data = new HashMap<>();
                data.put("email", email);
                response.put("data", data);

                return ResponseEntity.ok(response);

            } else {
                log.warn("사용자 이메일 조회 실패 - 사용자 없음: userId={}", userId);

                // 404 응답
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "사용자를 찾을 수 없습니다");
                response.put("timestamp", LocalDateTime.now());
                response.put("status", 404);

                return ResponseEntity.status(404).body(response);
            }

        } catch (Exception e) {
            log.error("사용자 이메일 조회 중 오류 발생: userId={}, error={}", userId, e.getMessage(), e);

            // 500 응답
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "INTERNAL_SERVER_ERROR");
            response.put("message", "서버 내부 오류가 발생했습니다: " + e.getMessage());
            response.put("timestamp", LocalDateTime.now());
            response.put("status", 500);

            return ResponseEntity.status(500).body(response);
        }
    }
}