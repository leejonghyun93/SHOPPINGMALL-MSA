package org.kosa.authservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kosa.authservice.dto.*;
import org.kosa.authservice.security.AuthResponse;
import org.kosa.authservice.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            String userId = request.getUserId();
            String password = request.getPassword();

            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(AuthResponse.builder()
                                .success(false)
                                .message("사용자 ID를 입력해주세요.")
                                .build());
            }

            if (password == null || password.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(AuthResponse.builder()
                                .success(false)
                                .message("비밀번호를 입력해주세요.")
                                .build());
            }

            AuthResponse response = authService.login(userId, password);

            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AuthResponse.builder()
                            .success(false)
                            .message("로그인 처리 중 오류가 발생했습니다: " + e.getMessage())
                            .build());
        }
    }

    @PostMapping("/validate")
    public ResponseEntity<AuthResponse> validateToken(@RequestHeader("Authorization") String token) {
        try {
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            AuthResponse response = authService.validateToken(token);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(AuthResponse.builder()
                            .success(false)
                            .message("토큰이 유효하지 않습니다")
                            .build());
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<AuthResponse> logout(@RequestHeader("Authorization") String token) {
        try {
            AuthResponse response = authService.logout(token);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.ok(AuthResponse.builder()
                    .success(true)
                    .message("로그아웃되었습니다")
                    .build());
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@RequestHeader("Authorization") String token) {
        try {
            if (token == null || token.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(AuthResponse.builder()
                                .success(false)
                                .message("Authorization 헤더가 필요합니다")
                                .build());
            }

            String cleanToken = token.startsWith("Bearer ") ? token.substring(7) : token;
            AuthResponse response = authService.refreshToken(cleanToken);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(AuthResponse.builder()
                            .success(false)
                            .message("토큰 갱신에 실패했습니다: " + e.getMessage())
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(AuthResponse.builder()
                            .success(false)
                            .message("서버 내부 오류가 발생했습니다")
                            .build());
        }
    }

    @PostMapping("/findPassword")
    public ResponseEntity<AuthResponse> findPassword(@Valid @RequestBody FindPasswordRequest request) {
        try {
            AuthResponse response = authService.findPassword(request);

            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(AuthResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(AuthResponse.builder()
                            .success(false)
                            .message("서버 내부 오류가 발생했습니다. 잠시 후 다시 시도해주세요.")
                            .build());
        }
    }

    @PostMapping("/verifyResetCode")
    public ResponseEntity<AuthResponse> verifyResetCode(@RequestBody VerifyResetCodeRequest request) {
        try {
            AuthResponse response = authService.verifyResetCode(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(AuthResponse.builder()
                            .success(false)
                            .message("인증번호 검증에 실패했습니다.")
                            .build());
        }
    }

    // 즉시 비밀번호 초기화용 엔드포인트
    @PostMapping("/resetPassword")
    public ResponseEntity<AuthResponse> resetPassword(@RequestBody ResetPasswordRequest request) {
        try {
            AuthResponse response = authService.resetPassword(request);

            if (response.isSuccess()) {
                return ResponseEntity.ok(AuthResponse.builder()
                        .success(true)
                        .message("비밀번호가 성공적으로 초기화되었습니다.")
                        .build());
            } else {
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(AuthResponse.builder()
                            .success(false)
                            .message("비밀번호 초기화에 실패했습니다.")
                            .build());
        }
    }

    // 즉시 비밀번호 초기화 (사용자 정보 검증 포함)
    @PostMapping("/resetPasswordImmediate")
    public ResponseEntity<AuthResponse> resetPasswordImmediate(@RequestBody ImmediateResetRequest request) {
        try {
            // 입력값 검증
            if (request.getUserid() == null || request.getUserid().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(AuthResponse.builder()
                                .success(false)
                                .message("아이디가 필요합니다.")
                                .build());
            }

            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(AuthResponse.builder()
                                .success(false)
                                .message("이메일이 필요합니다.")
                                .build());
            }

            if (request.getName() == null || request.getName().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(AuthResponse.builder()
                                .success(false)
                                .message("이름이 필요합니다.")
                                .build());
            }

            if (request.getNewPassword() == null || request.getNewPassword().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(AuthResponse.builder()
                                .success(false)
                                .message("새 비밀번호가 필요합니다.")
                                .build());
            }

            // 사용자 정보 검증 및 비밀번호 초기화
            AuthResponse response = authService.resetPasswordImmediate(request);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(AuthResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(AuthResponse.builder()
                            .success(false)
                            .message("서버 내부 오류가 발생했습니다.")
                            .build());
        }
    }
}