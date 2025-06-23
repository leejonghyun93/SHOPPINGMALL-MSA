package org.kosa.authservice.controller;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kosa.authservice.dto.LoginRequest;

import org.kosa.authservice.security.AuthResponse;
import org.kosa.authservice.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.bind.annotation.RestController;

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
}