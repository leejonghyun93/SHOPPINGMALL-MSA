package org.kosa.authservice.controller;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kosa.authservice.dto.*;

import org.kosa.authservice.security.AuthResponse;
import org.kosa.authservice.service.AuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    @Value("${user-service.url}")
    private String userServiceUrl;

    private final AuthService authService;
    private final PasswordEncoder passwordEncoder;
    private final RestTemplate restTemplate;

    /**
     * 로그인 (User Service와 연동)
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("로그인 요청: {}", loginRequest.getUserid());

        try {
            AuthResponse response = authService.login(loginRequest);
            log.info("로그인 성공: {}", loginRequest.getUserid());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("로그인 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(AuthResponse.builder()
                            .success(false)
                            .message("로그인에 실패했습니다: " + e.getMessage())
                            .build());
        }
    }

    /**
     * 토큰 검증
     */
    @PostMapping("/validate")
    public ResponseEntity<AuthResponse> validateToken(@RequestHeader("Authorization") String token) {
        log.debug("토큰 검증 요청");

        try {
            // Bearer 접두사 제거
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            AuthResponse response = authService.validateToken(token);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("토큰 검증 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(AuthResponse.builder()
                            .success(false)
                            .message("토큰이 유효하지 않습니다")
                            .build());
        }
    }

    /**
     * 로그아웃
     */
    @PostMapping("/logout")
    public ResponseEntity<AuthResponse> logout(@RequestHeader("Authorization") String token) {
        log.info("로그아웃 요청");

        // 실제로는 토큰을 블랙리스트에 추가하거나 Redis에서 제거
        return ResponseEntity.ok(AuthResponse.builder()
                .success(true)
                .message("로그아웃되었습니다")
                .build());
    }

    /**
     * 🔥 토큰 갱신 - 상세한 디버깅 추가
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@RequestHeader("Authorization") String token) {
        log.info("🔄 토큰 갱신 요청 시작");

        try {
            // 🔥 요청 정보 상세 로깅
            log.info("📤 받은 Authorization 헤더: {}", token != null ? token.substring(0, Math.min(30, token.length())) + "..." : "null");

            if (token == null || token.trim().isEmpty()) {
                log.error("❌ Authorization 헤더가 비어있음");
                return ResponseEntity.badRequest()
                        .body(AuthResponse.builder()
                                .success(false)
                                .message("Authorization 헤더가 필요합니다")
                                .build());
            }

            // Bearer 접두사 처리
            String cleanToken;
            if (token.startsWith("Bearer ")) {
                cleanToken = token.substring(7);
                log.info("✅ Bearer 접두사 제거됨");
            } else {
                cleanToken = token;
                log.info("⚠️ Bearer 접두사 없음, 원본 토큰 사용");
            }

            log.info("🔍 처리할 토큰 길이: {}", cleanToken.length());
            log.info("🔍 토큰 시작 부분: {}", cleanToken.substring(0, Math.min(50, cleanToken.length())) + "...");

            // AuthService의 refreshToken 호출
            AuthResponse response = authService.refreshToken(cleanToken);

            log.info("✅ 토큰 갱신 성공 - 새 토큰 길이: {}", response.getToken() != null ? response.getToken().length() : 0);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("❌ 토큰 갱신 실패 (잘못된 인자): {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(AuthResponse.builder()
                            .success(false)
                            .message("토큰 갱신에 실패했습니다: " + e.getMessage())
                            .build());
        } catch (Exception e) {
            log.error("❌ 토큰 갱신 실패 (예상치 못한 오류): {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(AuthResponse.builder()
                            .success(false)
                            .message("서버 내부 오류가 발생했습니다")
                            .build());
        }
    }

    @PostMapping("/findPassword")
    public ResponseEntity<AuthResponse> findPassword(@Valid @RequestBody FindPasswordRequest request) {
        log.info("비밀번호 찾기 요청: 아이디={}, 이메일={}", request.getUserid(), request.getEmail());

        try {
            // AuthService의 findPassword 메서드 호출
            AuthResponse response = authService.findPassword(request);

            if (response.isSuccess()) {
                log.info("비밀번호 찾기 성공: 아이디={}, 이메일={}", request.getUserid(), request.getEmail());
                return ResponseEntity.ok(response);
            } else {
                log.warn("비밀번호 찾기 실패: {}", response.getMessage());
                return ResponseEntity.badRequest().body(response);
            }

        } catch (IllegalArgumentException e) {
            log.error("비밀번호 찾기 실패 - 잘못된 요청: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(AuthResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        } catch (Exception e) {
            log.error("비밀번호 찾기 실패 - 서버 오류: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(AuthResponse.builder()
                            .success(false)
                            .message("서버 내부 오류가 발생했습니다. 잠시 후 다시 시도해주세요.")
                            .build());

        }
    }

    @PostMapping("/verifyResetCode")
    public ResponseEntity<AuthResponse> verifyResetCode(@RequestBody VerifyResetCodeRequest request) {
        log.info("인증번호 검증 요청: 아이디={}", request.getUserid());

        try {
            AuthResponse response = authService.verifyResetCode(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("인증번호 검증 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(AuthResponse.builder()
                            .success(false)
                            .message("인증번호 검증에 실패했습니다.")
                            .build());
        }
    }

    /**
     * 비밀번호 재설정
     */
    @PostMapping("/resetPassword")
    public ResponseEntity<AuthResponse> resetPassword(@RequestBody ResetPasswordRequest request) {
        log.info("비밀번호 재설정 요청: 아이디={}", request.getUserid());

        try {
            AuthResponse response = authService.resetPassword(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("비밀번호 재설정 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(AuthResponse.builder()
                            .success(false)
                            .message("비밀번호 재설정에 실패했습니다.")
                            .build());
        }
    }
    /**
     * User Service에 비밀번호 업데이트 요청
     */
    private boolean updateUserPassword(String userid, String newPassword) {
        try {
            String url = userServiceUrl + "/users/" + userid + "/password";

            // 비밀번호 업데이트 요청 DTO
            UpdatePasswordRequest updateRequest = new UpdatePasswordRequest();
            updateRequest.setNewPassword(passwordEncoder.encode(newPassword)); // 🔥 암호화해서 전송

            ResponseEntity<String> response = restTemplate.postForEntity(url, updateRequest, String.class);

            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.error("비밀번호 업데이트 요청 실패: userid={}, error={}", userid, e.getMessage());
            return false;
        }
    }

    /**
     * 임시 토큰 생성
     */
    private String generateTempToken(String userid) {
        return "TEMP_" + userid + "_" + System.currentTimeMillis();
    }

// AuthController에 추가할 테스트용 엔드포인트 (data 필드 제거)

    @PostMapping("/findPassword/test")
    public ResponseEntity<AuthResponse> testFindPassword(@RequestBody FindPasswordRequest request) {
        log.info("🔥 테스트 비밀번호 찾기 요청: userid={}, email={}", request.getUserid(), request.getEmail());

        try {
            // 간단한 검증
            if (request.getUserid() == null || request.getUserid().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(AuthResponse.builder()
                                .success(false)
                                .message("아이디가 필요합니다")
                                .build());
            }

            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(AuthResponse.builder()
                                .success(false)
                                .message("이메일이 필요합니다")
                                .build());
            }

            // 🔥 하드코딩된 테스트 사용자
            if ("testuser".equals(request.getUserid()) && "test@example.com".equals(request.getEmail())) {
                log.info("✅ 테스트 사용자 일치!");

                // 성공 응답 (data 필드 제거)
                return ResponseEntity.ok(AuthResponse.builder()
                        .success(true)
                        .message("인증번호가 이메일로 발송되었습니다. 10분 내에 입력해주세요.")
                        .build());
            } else {
                log.info("⚠️ 테스트 사용자 불일치");
                return ResponseEntity.badRequest()
                        .body(AuthResponse.builder()
                                .success(false)
                                .message("테스트: 'testuser' + 'test@example.com'을 입력해보세요")
                                .build());
            }

        } catch (Exception e) {
            log.error("❌ 테스트 중 오류: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(AuthResponse.builder()
                            .success(false)
                            .message("서버 오류: " + e.getMessage())
                            .build());
        }
    }
}