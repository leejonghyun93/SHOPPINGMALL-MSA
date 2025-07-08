package org.kosa.userservice.userController;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kosa.userservice.dto.ApiResponse;
import org.kosa.userservice.dto.AuthResponse;
import org.kosa.userservice.dto.UserDto;
import org.kosa.userservice.dto.UserSessionDto;
import org.kosa.userservice.entity.Member;
import org.kosa.userservice.userService.TokenValidationService;
import org.kosa.userservice.userService.UserCacheService;
import org.kosa.userservice.userService.UserService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserApiController {

    private final UserService userService;
    private final TokenValidationService tokenValidationService;
    private final UserCacheService userCacheService;
    private final RedisTemplate<String, Object> redisTemplate;

    @GetMapping("/findId")
    public ResponseEntity<?> findId(
            @RequestParam("name") String name,
            @RequestParam("email") String email) {

        log.info("아이디 찾기 요청 - name: {}, email: {}***", name,
                email.length() > 3 ? email.substring(0, 3) : email);

        try {
            if (name == null || name.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "이름을 입력해주세요."
                ));
            }

            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "이메일을 입력해주세요."
                ));
            }

            Optional<UserDto> userOpt = userService.getMemberByNameAndEmail(name.trim(), email.trim());

            if (userOpt.isPresent()) {
                UserDto user = userOpt.get();
                log.info("아이디 찾기 성공 - userId: {}", user.getUserId());

                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "아이디 찾기 성공",
                        "userId", user.getUserId(),
                        "timestamp", LocalDateTime.now()
                ));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                        "success", false,
                        "message", "입력하신 정보와 일치하는 계정을 찾을 수 없습니다.",
                        "timestamp", LocalDateTime.now()
                ));
            }

        } catch (Exception e) {
            log.error("아이디 찾기 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.",
                    "timestamp", LocalDateTime.now()
            ));
        }
    }
    @GetMapping("/profile/{userId}")
    public ResponseEntity<?> getUserFromCache(@PathVariable String userId) {
        try {
            log.debug("🔍 캐시에서 사용자 정보 조회: userId={}", userId);

            // 캐시에서 조회 (없으면 DB에서 조회 후 캐시 저장)
            Optional<UserSessionDto> sessionOpt = userCacheService.getUserSessionWithFallback(userId);

            if (sessionOpt.isPresent()) {
                UserSessionDto session = sessionOpt.get();

                // UserDto로 변환
                UserDto userDto = UserDto.builder()
                        .userId(session.getUserId())
                        .name(session.getName())
                        .email(session.getEmail())
                        .phone(session.getPhone())
                        .gradeId(session.getGradeId())
                        .status(session.getStatus())
                        .birthDate(session.getBirthDate())
                        .build();

                return ResponseEntity.ok(ApiResponse.builder()
                        .success(true)
                        .message("사용자 정보 조회 성공")
                        .data(userDto)
                        .build());
            } else {
                log.warn("⚠️ 사용자 정보를 찾을 수 없음: userId={}", userId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.builder()
                                .success(false)
                                .message("사용자 정보를 찾을 수 없습니다")
                                .build());
            }

        } catch (Exception e) {
            log.error("❌ 사용자 정보 조회 실패: userId={}, error={}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message("사용자 정보 조회 실패")
                            .build());
        }
    }

    /**
     * 사용자 세션 캐시 저장 (Auth-Service에서 호출)
     */
    @PostMapping("/cache/{userId}")
    public ResponseEntity<?> cacheUserSession(@PathVariable String userId) {
        try {
            log.info("🔍 사용자 세션 캐시 저장 요청: userId={}", userId);

            userCacheService.cacheUserSession(userId);

            return ResponseEntity.ok(ApiResponse.builder()
                    .success(true)
                    .message("사용자 세션 캐시 저장 완료")
                    .build());

        } catch (Exception e) {
            log.error("❌ 사용자 세션 캐시 저장 실패: userId={}, error={}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message("캐시 저장 실패")
                            .build());
        }
    }
    /**
     * 사용자 세션 정보 조회 (Redis 우선, DB fallback)
     */
    @GetMapping("/session/{userId}")
    public ResponseEntity<?> getUserSession(@PathVariable String userId) {
        try {
            log.info("📋 사용자 세션 정보 조회 요청: userId={}", userId);

            Optional<UserSessionDto> sessionOpt = userCacheService.getUserSessionWithFallback(userId);

            if (sessionOpt.isPresent()) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "사용자 세션 정보 조회 성공",
                        "data", sessionOpt.get()
                ));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of(
                                "success", false,
                                "message", "사용자 세션 정보를 찾을 수 없습니다"
                        ));
            }
        } catch (Exception e) {
            log.error("❌ 사용자 세션 정보 조회 실패: userId={}, error={}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "사용자 세션 정보 조회 실패"
                    ));
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

        // 토큰 검증
        String authenticatedUserId = tokenValidationService.validateAndExtractUserId(
                request.getHeader("Authorization")
        );

        if (authenticatedUserId == null) {
            log.warn("토큰 검증 실패");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (!authenticatedUserId.equals(userId)) {
            log.warn("User ID mismatch - URL: {}, Token: {}", userId, authenticatedUserId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        if (!userService.isMemberExists(userId)) {
            log.warn("User does not exist: {}", userId);
            return ResponseEntity.notFound().build();
        }

        log.info("Deleting user: {}", userId);
        userService.deleteMember(userId);
        return ResponseEntity.noContent().build();
    }

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
            @RequestBody UserDto userDto,
            HttpServletRequest request) {

        // 토큰 검증
        String authenticatedUserId = tokenValidationService.validateAndExtractUserId(
                request.getHeader("Authorization")
        );

        if (authenticatedUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("인증이 필요합니다.");
        }

        if (!authenticatedUserId.equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("본인만 수정할 수 있습니다.");
        }

        if (!userId.equals(userDto.getUserId())) {
            return ResponseEntity.badRequest().body("User ID mismatch");
        }

        try {
            userService.updateMember(userDto);
            return ResponseEntity.ok("User updated successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Update failed");
        }
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

            // 토큰 검증
            String userId = tokenValidationService.validateAndExtractUserId(
                    httpRequest.getHeader("Authorization")
            );

            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "인증이 필요합니다."));
            }

            log.info("비밀번호 검증 요청 - userId: {}", userId);

            Optional<UserDto> userOpt = userService.getMemberDetail(userId);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "사용자를 찾을 수 없습니다."));
            }

            UserDto user = userOpt.get();
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
            // 토큰 검증
            String userId = tokenValidationService.validateAndExtractUserId(
                    httpRequest.getHeader("Authorization")
            );

            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "인증이 필요합니다."));
            }

            log.info("프로필 조회 요청 - userId: {}", userId);

            Optional<UserDto> userOpt = userService.getMemberDetail(userId);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("success", false, "message", "사용자를 찾을 수 없습니다."));
            }

            UserDto user = userOpt.get();

            // ✅ success, data 구조로 통일
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "프로필 조회 성공");

            Map<String, Object> userData = new HashMap<>();
            userData.put("userId", user.getUserId());
            userData.put("name", user.getName());
            userData.put("email", user.getEmail());
            userData.put("phone", user.getPhone());
            userData.put("birthDate", user.getBirthDate());
            userData.put("address", user.getAddress());
            userData.put("zipcode", user.getZipcode());
            userData.put("gender", user.getGender());
            userData.put("nickname", user.getName());
            response.put("data", userData);

            log.info("프로필 조회 성공 - userId: {}, name: {}", userId, user.getName());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("프로필 조회 중 오류: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "프로필 정보를 불러올 수 없습니다."
                    ));
        }
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateUserProfile(
            @RequestBody UserDto userDto,
            HttpServletRequest httpRequest) {

        try {
            // 토큰 검증
            String userId = tokenValidationService.validateAndExtractUserId(
                    httpRequest.getHeader("Authorization")
            );

            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "인증이 필요합니다."));
            }

            log.info("프로필 수정 요청 - userId: {}", userId);

            if (userDto.getName() == null || userDto.getName().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "이름은 필수 항목입니다."));
            }

            if (userDto.getEmail() == null || userDto.getEmail().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "이메일은 필수 항목입니다."));
            }

            userDto.setUserId(userId);
            userService.updateMember(userDto);

            // 🔥 사용자 정보 변경 시 캐시 갱신
            userCacheService.refreshUserSession(userId);

            log.info("프로필 수정 및 캐시 갱신 완료 - userId: {}", userId);
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

            String email = userService.getUserEmailByUserId(userId);

            if (email != null && !email.isEmpty()) {
                log.info("사용자 이메일 조회 성공: userId={}", userId);

                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "사용자 이메일 조회 성공",
                        "data", Map.of("email", email)
                ));
            } else {
                return ResponseEntity.status(404).body(Map.of(
                        "success", false,
                        "message", "사용자를 찾을 수 없습니다",
                        "timestamp", LocalDateTime.now(),
                        "status", 404
                ));
            }

        } catch (Exception e) {
            log.error("사용자 이메일 조회 중 오류 발생: userId={}, error={}", userId, e.getMessage(), e);

            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", "INTERNAL_SERVER_ERROR",
                    "message", "서버 내부 오류가 발생했습니다: " + e.getMessage(),
                    "timestamp", LocalDateTime.now(),
                    "status", 500
            ));
        }
    }
    // UserApiController.java에 추가
    @GetMapping("/redis/health")
    public ResponseEntity<?> checkRedisHealth() {
        try {
            // Redis 연결 테스트
            redisTemplate.opsForValue().set("health:check", "OK", Duration.ofSeconds(10));
            String result = (String) redisTemplate.opsForValue().get("health:check");

            if ("OK".equals(result)) {
                return ResponseEntity.ok(Map.of(
                        "redis", "Connected",
                        "status", "OK",
                        "timestamp", LocalDateTime.now()
                ));
            } else {
                return ResponseEntity.status(500).body(Map.of(
                        "redis", "Disconnected",
                        "status", "ERROR"
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "redis", "Error",
                    "status", "ERROR",
                    "error", e.getMessage()
            ));
        }
    }

    @PostMapping("/social")
    public ResponseEntity<?> createOrUpdateSocialUser(@RequestBody Map<String, Object> socialUserData) {
        try {
            log.info("🔍 소셜 사용자 생성/업데이트 요청: provider={}, socialId={}",
                    socialUserData.get("provider"), socialUserData.get("socialId"));

            // 요청 데이터 검증
            String socialId = (String) socialUserData.get("socialId");
            String provider = (String) socialUserData.get("provider");
            String email = (String) socialUserData.get("email");
            String name = (String) socialUserData.get("name");
            String nickname = (String) socialUserData.get("nickname");

            if (socialId == null || provider == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "socialId와 provider는 필수 항목입니다."
                ));
            }

            // 소셜 로그인 사용자 처리
            UserDto userDto = userService.createOrUpdateSocialUser(socialUserData);

            log.info("✅ 소셜 사용자 처리 완료 - userId: {}, provider: {}",
                    userDto.getUserId(), provider);

            return ResponseEntity.ok(userDto);

        } catch (Exception e) {
            log.error("💥 소셜 사용자 처리 중 오류", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "error", "INTERNAL_SERVER_ERROR",
                    "message", "서버 내부 오류가 발생했습니다.",
                    "timestamp", LocalDateTime.now(),
                    "status", 500
            ));
        }
    }
}