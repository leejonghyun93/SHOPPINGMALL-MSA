package org.kosa.authservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kosa.authservice.security.AuthResponse;
import org.kosa.authservice.service.SocialAuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class SocialAuthController {

    private final SocialAuthService socialAuthService;

    @Value("${frontend.url:http://localhost:5173}")
    private String frontendUrl;

    /**
     * 소셜 로그인 콜백 처리 (카카오, 네이버 통합)
     * 프론트엔드로 리다이렉트하면서 토큰 전달
     */
    @GetMapping("/callback")
    public void handleSocialCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String error_description,
            HttpServletResponse response) throws IOException {

        log.info("🔍 소셜 로그인 콜백 받음 - code: {}, state: {}, error: {}",
                code != null ? "present" : "null", state, error);

        try {
            // 에러가 있는 경우 처리
            if (error != null) {
                log.error("❌ 소셜 로그인 에러 - error: {}, description: {}", error, error_description);

                String errorMessage = "소셜 로그인 중 오류가 발생했습니다.";
                if ("access_denied".equals(error)) {
                    errorMessage = "소셜 로그인을 취소하셨습니다.";
                }

                redirectToFrontendWithError(response, errorMessage);
                return;
            }

            // code가 없는 경우
            if (code == null || code.trim().isEmpty()) {
                log.error("❌ Authorization code가 없음");
                redirectToFrontendWithError(response, "인증 코드를 받지 못했습니다.");
                return;
            }

            // 소셜 로그인 처리
            AuthResponse authResponse = socialAuthService.processSocialLogin(code, state);

            if (authResponse.isSuccess()) {
                log.info("✅ 소셜 로그인 성공 - userId: {}", authResponse.getUserId());

                // 성공 시 토큰과 함께 프론트엔드로 리다이렉트
                redirectToFrontendWithSuccess(response, authResponse.getToken());
            } else {
                log.error("❌ 소셜 로그인 실패 - message: {}", authResponse.getMessage());
                redirectToFrontendWithError(response, authResponse.getMessage());
            }

        } catch (Exception e) {
            log.error("💥 소셜 로그인 콜백 처리 중 예외 발생", e);
            redirectToFrontendWithError(response, "로그인 처리 중 오류가 발생했습니다.");
        }
    }

    /**
     * 소셜 로그인 API (선택적 - REST API로 직접 호출하는 경우)
     */
    @PostMapping("/social/login")
    public ResponseEntity<AuthResponse> socialLogin(
            @RequestParam String code,
            @RequestParam(required = false) String state) {

        log.info("🔍 소셜 로그인 API 호출 - code: {}, state: {}",
                code != null ? "present" : "null", state);

        try {
            if (code == null || code.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(AuthResponse.builder()
                                .success(false)
                                .message("Authorization code가 필요합니다.")
                                .build());
            }

            AuthResponse authResponse = socialAuthService.processSocialLogin(code, state);

            if (authResponse.isSuccess()) {
                log.info("✅ 소셜 로그인 API 성공 - userId: {}", authResponse.getUserId());
                return ResponseEntity.ok(authResponse);
            } else {
                log.warn("⚠️ 소셜 로그인 API 실패 - message: {}", authResponse.getMessage());
                return ResponseEntity.badRequest().body(authResponse);
            }

        } catch (Exception e) {
            log.error("💥 소셜 로그인 API 처리 중 예외 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AuthResponse.builder()
                            .success(false)
                            .message("로그인 처리 중 오류가 발생했습니다.")
                            .build());
        }
    }

    /**
     * 소셜 로그인 성공 시 프론트엔드로 리다이렉트
     */
    private void redirectToFrontendWithSuccess(HttpServletResponse response, String token) throws IOException {
        try {
            String encodedToken = URLEncoder.encode(token, StandardCharsets.UTF_8);
            String redirectUrl = String.format("%s/login?token=%s", frontendUrl, encodedToken);

            log.info("🔄 소셜 로그인 성공 리다이렉트: {}", frontendUrl + "/login?token=***");
            response.sendRedirect(redirectUrl);

        } catch (Exception e) {
            log.error("💥 성공 리다이렉트 처리 중 오류", e);
            redirectToFrontendWithError(response, "로그인 완료 처리 중 오류가 발생했습니다.");
        }
    }

    /**
     * 소셜 로그인 실패 시 프론트엔드로 리다이렉트
     */
    private void redirectToFrontendWithError(HttpServletResponse response, String errorMessage) throws IOException {
        try {
            String encodedError = URLEncoder.encode(errorMessage, StandardCharsets.UTF_8);
            String redirectUrl = String.format("%s/login?error=%s", frontendUrl, encodedError);

            log.info("🔄 소셜 로그인 실패 리다이렉트: {}", frontendUrl + "/login?error=***");
            response.sendRedirect(redirectUrl);

        } catch (Exception e) {
            log.error("💥 실패 리다이렉트 처리 중 오류", e);
            response.sendRedirect(frontendUrl + "/login");
        }
    }

    /**
     * 소셜 로그인 설정 확인
     */
    @GetMapping("/social/config")
    public ResponseEntity<?> getSocialConfig() {
        log.debug("🔍 소셜 로그인 설정 확인 요청");

        try {
            return ResponseEntity.ok("소셜 로그인 설정 확인 완료");
        } catch (Exception e) {
            log.error("💥 소셜 로그인 설정 확인 중 오류", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("설정 확인 중 오류가 발생했습니다.");
        }
    }
}