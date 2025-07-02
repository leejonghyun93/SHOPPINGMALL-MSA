package org.kosa.authservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kosa.authservice.dto.*;
import org.kosa.authservice.security.AuthResponse;
import org.kosa.authservice.service.AuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    /**
     * 로그인 (User Service와 연동)
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            // 🔥 요청 데이터 확인
            log.info("🔍 로그인 요청 - userid: '{}', passwd: '{}'",
                    request.getUserId(), request.getPassword() != null ? "***" : "null");

            // 🔥 필드 검증
            String userId = request.getUserId();
            String password = request.getPassword();

            if (userId == null || userId.trim().isEmpty()) {
                log.warn("⚠️ userId가 null이거나 비어있습니다!");
                return ResponseEntity.badRequest()
                        .body(AuthResponse.builder()
                                .success(false)
                                .message("사용자 ID를 입력해주세요.")
                                .build());
            }

            if (password == null || password.trim().isEmpty()) {
                log.warn("⚠️ password가 null이거나 비어있습니다!");
                return ResponseEntity.badRequest()
                        .body(AuthResponse.builder()
                                .success(false)
                                .message("비밀번호를 입력해주세요.")
                                .build());
            }

            // AuthService에서 User Service 호출 및 JWT 생성 처리
            AuthResponse response = authService.login(userId, password);

            if (response.isSuccess()) {
                log.info("로그인 성공: {}", userId);
                return ResponseEntity.ok(response);
            } else {
                log.warn("로그인 실패: {}", response.getMessage());
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            log.error("로그인 처리 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AuthResponse.builder()
                            .success(false)
                            .message("로그인 처리 중 오류가 발생했습니다: " + e.getMessage())
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

        try {
            AuthResponse response = authService.logout(token);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("로그아웃 실패: {}", e.getMessage());
            return ResponseEntity.ok(AuthResponse.builder()
                    .success(true)
                    .message("로그아웃되었습니다")
                    .build());
        }
    }

    /**
     * 토큰 갱신
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@RequestHeader("Authorization") String token) {
        log.info("🔄 토큰 갱신 요청 시작");

        try {
            if (token == null || token.trim().isEmpty()) {
                log.error("❌ Authorization 헤더가 비어있음");
                return ResponseEntity.badRequest()
                        .body(AuthResponse.builder()
                                .success(false)
                                .message("Authorization 헤더가 필요합니다")
                                .build());
            }

            // Bearer 접두사 처리
            String cleanToken = token.startsWith("Bearer ") ? token.substring(7) : token;

            AuthResponse response = authService.refreshToken(cleanToken);
            log.info("✅ 토큰 갱신 성공");

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

    /**
     * 비밀번호 찾기
     */
    @PostMapping("/findPassword")
    public ResponseEntity<AuthResponse> findPassword(@Valid @RequestBody FindPasswordRequest request) {
        log.info("비밀번호 찾기 요청: 아이디={}, 이메일={}", request.getUserid(), request.getEmail());

        try {
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

    /**
     * 인증번호 검증
     */
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
     * 테스트용 비밀번호 찾기
     */
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