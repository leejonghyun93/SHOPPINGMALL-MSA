package org.kosa.authservice.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kosa.authservice.dto.*;
import org.kosa.authservice.security.AuthResponse;
import org.kosa.authservice.util.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    @Value("${user-service.url:http://user-service:8103}")
    private String userServiceUrl;

    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final RestTemplate restTemplate;

    private final Map<String, String> resetCodes = new ConcurrentHashMap<>();

    @CircuitBreaker(name = "authService", fallbackMethod = "loginFallback")
    @Retry(name = "authService")
    @TimeLimiter(name = "authService")
    public CompletableFuture<AuthResponse> loginAsync(String userId, String password) {
        return CompletableFuture.supplyAsync(() -> login(userId, password));
    }

    public AuthResponse login(String userId, String password) {
        try {
            if (userId == null || userId.trim().isEmpty()) {
                return AuthResponse.builder()
                        .success(false)
                        .message("사용자 ID가 입력되지 않았습니다.")
                        .build();
            }

            if (password == null || password.trim().isEmpty()) {
                return AuthResponse.builder()
                        .success(false)
                        .message("비밀번호가 입력되지 않았습니다.")
                        .build();
            }

            UserDto user = getUserFromUserService(userId);
            if (user == null) {
                return AuthResponse.builder()
                        .success(false)
                        .message("존재하지 않는 사용자입니다.")
                        .build();
            }

            if (!passwordEncoder.matches(password, user.getPassword())) {
                return AuthResponse.builder()
                        .success(false)
                        .message("비밀번호가 일치하지 않습니다.")
                        .build();
            }

            cacheUserSessionInUserService(userId);

            String actualName = determineUserName(user);

            String accessToken = jwtUtil.generateToken(user.getUserId(), "USER", actualName);
            String refreshToken = jwtUtil.generateRefreshToken(user.getUserId());

            return AuthResponse.builder()
                    .success(true)
                    .message("로그인 성공")
                    .token(accessToken)
                    .userId(userId)
                    .username(user.getUserId())
                    .name(actualName)
                    .email(user.getEmail())
                    .phone(user.getPhone())
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("로그인 처리 중 오류가 발생했습니다.");
        }
    }

    private String determineUserName(UserDto user) {
        if (user.getName() != null && !user.getName().trim().isEmpty() &&
                !user.getName().equals("사용자") && !user.getName().equals("소셜사용자")) {
            return user.getName().trim();
        }
        return user.getUserId();
    }

    @CircuitBreaker(name = "userService", fallbackMethod = "resetPasswordImmediateFallback")
    @Retry(name = "userService")
    public AuthResponse resetPasswordImmediate(ImmediateResetRequest request) {
        try {
            if (!verifyUserInfo(request.getUserid(), request.getEmail(), request.getName())) {
                return AuthResponse.builder()
                        .success(false)
                        .message("입력하신 정보와 일치하는 계정을 찾을 수 없습니다.")
                        .build();
            }

            boolean updated = updatePasswordInUserService(request.getUserid(), request.getNewPassword());

            if (updated) {
                return AuthResponse.builder()
                        .success(true)
                        .message("비밀번호가 성공적으로 초기화되었습니다.")
                        .build();
            } else {
                return AuthResponse.builder()
                        .success(false)
                        .message("비밀번호 초기화에 실패했습니다.")
                        .build();
            }

        } catch (Exception e) {
            throw new RuntimeException("비밀번호 즉시 초기화 처리 중 오류가 발생했습니다.");
        }
    }

    @CircuitBreaker(name = "userService", fallbackMethod = "verifyUserInfoFallback")
    @Retry(name = "userService")
    private boolean verifyUserInfo(String userId, String email, String name) {
        try {
            String url = userServiceUrl + "/api/users/findId";
            String fullUrl = url + "?name=" + name + "&email=" + email;

            ResponseEntity<Map> response = restTemplate.getForEntity(fullUrl, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                Boolean success = (Boolean) responseBody.get("success");
                String foundUserId = (String) responseBody.get("userId");

                return Boolean.TRUE.equals(success) && userId.equals(foundUserId);
            }

            return false;
        } catch (Exception e) {
            throw new RuntimeException("사용자 정보 검증 실패: " + e.getMessage());
        }
    }

    @CircuitBreaker(name = "userService", fallbackMethod = "cacheUserSessionInUserServiceFallback")
    @Retry(name = "userService")
    private void cacheUserSessionInUserService(String userId) {
        try {
            String url = userServiceUrl + "/api/users/cache/" + userId;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("사용자 세션 캐시 저장 실패");
            }
        } catch (Exception e) {
            throw new RuntimeException("사용자 세션 캐시 저장 실패: " + e.getMessage());
        }
    }

    @CircuitBreaker(name = "authService", fallbackMethod = "validateTokenFallback")
    @Retry(name = "authService")
    public AuthResponse validateToken(String token) {
        try {
            if (token == null || token.trim().isEmpty()) {
                return AuthResponse.builder()
                        .success(false)
                        .message("토큰이 제공되지 않았습니다")
                        .build();
            }

            if (jwtUtil.validateAccessToken(token)) {
                String userId = jwtUtil.getUserIdFromToken(token);
                String role = jwtUtil.getRoleFromToken(token);

                UserDto sessionUser = getUserFromUserServiceSession(userId);

                if (sessionUser != null) {
                    return AuthResponse.builder()
                            .success(true)
                            .message("유효한 토큰입니다")
                            .userId(userId)
                            .username(userId)
                            .name(sessionUser.getName())
                            .email(sessionUser.getEmail())
                            .phone(sessionUser.getPhone())
                            .build();
                }

                UserDto dbUser = getUserFromUserService(userId);

                if (dbUser != null) {
                    return AuthResponse.builder()
                            .success(true)
                            .message("유효한 토큰입니다")
                            .userId(userId)
                            .username(userId)
                            .name(dbUser.getName())
                            .email(dbUser.getEmail())
                            .phone(dbUser.getPhone())
                            .build();
                } else {
                    return AuthResponse.builder()
                            .success(true)
                            .message("유효한 토큰입니다")
                            .userId(userId)
                            .username(userId)
                            .build();
                }
            } else {
                return AuthResponse.builder()
                        .success(false)
                        .message("유효하지 않은 토큰입니다")
                        .build();
            }
        } catch (Exception e) {
            throw new RuntimeException("토큰 검증 중 오류가 발생했습니다");
        }
    }

    @CircuitBreaker(name = "userService", fallbackMethod = "getUserFromUserServiceSessionFallback")
    @Retry(name = "userService")
    private UserDto getUserFromUserServiceSession(String userId) {
        try {
            String url = userServiceUrl + "/api/users/session/" + userId;

            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                Boolean success = (Boolean) responseBody.get("success");

                if (Boolean.TRUE.equals(success)) {
                    Map<String, Object> data = (Map<String, Object>) responseBody.get("data");

                    if (data != null) {
                        return UserDto.builder()
                                .userId((String) data.get("userId"))
                                .name((String) data.get("name"))
                                .email((String) data.get("email"))
                                .phone((String) data.get("phone"))
                                .gradeId((String) data.get("gradeId"))
                                .status((String) data.get("status"))
                                .build();
                    }
                }
            }

            return null;

        } catch (Exception e) {
            throw new RuntimeException("세션 조회 실패: " + e.getMessage());
        }
    }

    @CircuitBreaker(name = "authService", fallbackMethod = "refreshTokenFallback")
    @Retry(name = "authService")
    public AuthResponse refreshToken(String refreshToken) {
        try {
            if (refreshToken == null || refreshToken.trim().isEmpty()) {
                return AuthResponse.builder()
                        .success(false)
                        .message("리프레시 토큰이 제공되지 않았습니다")
                        .build();
            }

            String newAccessToken = jwtUtil.refreshAccessToken(refreshToken);
            String userId = jwtUtil.getUserIdFromToken(newAccessToken);

            return AuthResponse.builder()
                    .success(true)
                    .message("토큰이 갱신되었습니다")
                    .token(newAccessToken)
                    .userId(userId)
                    .username(userId)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("토큰 갱신에 실패했습니다");
        }
    }

    public AuthResponse logout(String token) {
        try {
            if (token != null && jwtUtil.validateAccessToken(token)) {
                String userId = jwtUtil.getUserIdFromToken(token);
            }

            return AuthResponse.builder()
                    .success(true)
                    .message("로그아웃되었습니다")
                    .build();
        } catch (Exception e) {
            return AuthResponse.builder()
                    .success(true)
                    .message("로그아웃되었습니다")
                    .build();
        }
    }

    @CircuitBreaker(name = "userService", fallbackMethod = "getUserProfileFallback")
    @Retry(name = "userService")
    public AuthResponse getUserProfile(String userId) {
        try {
            if (userId == null || userId.trim().isEmpty()) {
                return AuthResponse.builder()
                        .success(false)
                        .message("사용자 ID가 제공되지 않았습니다")
                        .build();
            }

            UserDto user = getUserFromUserService(userId);
            if (user != null) {
                return AuthResponse.builder()
                        .success(true)
                        .message("사용자 정보 조회 성공")
                        .userId(user.getUserId())
                        .username(user.getUserId())
                        .name(user.getName())
                        .email(user.getEmail())
                        .phone(user.getPhone())
                        .build();
            } else {
                return AuthResponse.builder()
                        .success(false)
                        .message("사용자 정보를 찾을 수 없습니다")
                        .build();
            }
        } catch (Exception e) {
            throw new RuntimeException("사용자 정보 조회 중 오류가 발생했습니다");
        }
    }

    @CircuitBreaker(name = "userService", fallbackMethod = "findPasswordFallback")
    @Retry(name = "userService")
    public AuthResponse findPassword(FindPasswordRequest request) {
        try {
            if (request == null || request.getUserid() == null || request.getEmail() == null) {
                return AuthResponse.builder()
                        .success(false)
                        .message("필수 정보가 입력되지 않았습니다.")
                        .build();
            }

            UserDto user = getUserFromUserService(request.getUserid());

            if (user == null) {
                return AuthResponse.builder()
                        .success(false)
                        .message("해당 정보로 등록된 사용자를 찾을 수 없습니다.")
                        .build();
            }

            if (!request.getEmail().equals(user.getEmail())) {
                return AuthResponse.builder()
                        .success(false)
                        .message("해당 정보로 등록된 사용자를 찾을 수 없습니다.")
                        .build();
            }

            String resetCode = generateResetCode();
            resetCodes.put(request.getUserid(), resetCode);

            return AuthResponse.builder()
                    .success(true)
                    .message("인증번호가 이메일로 발송되었습니다. 10분 내에 입력해주세요.")
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("비밀번호 찾기 처리 중 오류가 발생했습니다.");
        }
    }

    public AuthResponse verifyResetCode(VerifyResetCodeRequest request) {
        try {
            if (request == null || request.getUserid() == null || request.getVerificationCode() == null) {
                return AuthResponse.builder()
                        .success(false)
                        .message("필수 정보가 입력되지 않았습니다.")
                        .build();
            }

            String storedCode = resetCodes.get(request.getUserid());

            if (storedCode == null) {
                return AuthResponse.builder()
                        .success(false)
                        .message("인증번호 요청 내역이 없습니다. 다시 요청해주세요.")
                        .build();
            }

            if (!storedCode.equals(request.getVerificationCode())) {
                return AuthResponse.builder()
                        .success(false)
                        .message("인증번호가 일치하지 않습니다.")
                        .build();
            }

            return AuthResponse.builder()
                    .success(true)
                    .message("인증번호가 확인되었습니다. 새 비밀번호를 설정해주세요.")
                    .build();
        } catch (Exception e) {
            return AuthResponse.builder()
                    .success(false)
                    .message("인증번호 확인 중 오류가 발생했습니다.")
                    .build();
        }
    }

    @CircuitBreaker(name = "userService", fallbackMethod = "resetPasswordFallback")
    @Retry(name = "userService")
    public AuthResponse resetPassword(ResetPasswordRequest request) {
        try {
            if (request == null || request.getUserid() == null ||
                    request.getVerificationCode() == null || request.getNewPassword() == null) {
                return AuthResponse.builder()
                        .success(false)
                        .message("필수 정보가 입력되지 않았습니다.")
                        .build();
            }

            String storedCode = resetCodes.get(request.getUserid());
            if (storedCode == null || !storedCode.equals(request.getVerificationCode())) {
                return AuthResponse.builder()
                        .success(false)
                        .message("유효하지 않은 인증번호입니다.")
                        .build();
            }

            if (!request.getNewPassword().equals(request.getConfirmPassword())) {
                return AuthResponse.builder()
                        .success(false)
                        .message("비밀번호와 비밀번호 확인이 일치하지 않습니다.")
                        .build();
            }

            boolean updated = updatePasswordInUserService(request.getUserid(), request.getNewPassword());

            if (updated) {
                resetCodes.remove(request.getUserid());

                return AuthResponse.builder()
                        .success(true)
                        .message("비밀번호가 성공적으로 변경되었습니다.")
                        .build();
            } else {
                return AuthResponse.builder()
                        .success(false)
                        .message("비밀번호 변경에 실패했습니다.")
                        .build();
            }

        } catch (Exception e) {
            throw new RuntimeException("비밀번호 재설정 중 오류가 발생했습니다.");
        }
    }

    @CircuitBreaker(name = "userService", fallbackMethod = "getUserFromUserServiceFallback")
    @Retry(name = "userService")
    private UserDto getUserFromUserService(String userId) {
        try {
            String url = userServiceUrl + "/api/users/" + userId;

            ResponseEntity<UserDto> response = restTemplate.getForEntity(url, UserDto.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            } else {
                throw new RuntimeException("사용자 정보 조회 실패");
            }
        } catch (Exception e) {
            throw new RuntimeException("User Service 호출 실패: " + e.getMessage());
        }
    }

    @CircuitBreaker(name = "userService", fallbackMethod = "updatePasswordInUserServiceFallback")
    @Retry(name = "userService")
    private boolean updatePasswordInUserService(String userId, String newPassword) {
        try {
            String url = userServiceUrl + "/api/users/" + userId + "/password";

            Map<String, String> request = new HashMap<>();
            request.put("newPassword", passwordEncoder.encode(newPassword));

            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            throw new RuntimeException("비밀번호 업데이트 실패: " + e.getMessage());
        }
    }

    private String generateResetCode() {
        return String.format("%06d", (int) (Math.random() * 1000000));
    }

    // Fallback Methods
    public CompletableFuture<AuthResponse> loginFallback(String userId, String password, Exception ex) {
        AuthResponse fallbackResponse = AuthResponse.builder()
                .success(false)
                .message("로그인 시스템에 일시적인 문제가 발생했습니다. 잠시 후 다시 시도해주세요.")
                .build();

        return CompletableFuture.completedFuture(fallbackResponse);
    }

    public void cacheUserSessionInUserServiceFallback(String userId, Exception ex) {
        // 캐시 실패 시 무시하고 계속 진행
    }

    public AuthResponse validateTokenFallback(String token, Exception ex) {
        return AuthResponse.builder()
                .success(false)
                .message("인증 시스템에 일시적인 문제가 발생했습니다.")
                .build();
    }

    public UserDto getUserFromUserServiceSessionFallback(String userId, Exception ex) {
        return null;
    }

    public AuthResponse refreshTokenFallback(String refreshToken, Exception ex) {
        return AuthResponse.builder()
                .success(false)
                .message("토큰 갱신 시스템에 일시적인 문제가 발생했습니다.")
                .build();
    }

    public AuthResponse getUserProfileFallback(String userId, Exception ex) {
        return AuthResponse.builder()
                .success(false)
                .message("사용자 정보 조회 시스템에 일시적인 문제가 발생했습니다.")
                .build();
    }

    public AuthResponse findPasswordFallback(FindPasswordRequest request, Exception ex) {
        return AuthResponse.builder()
                .success(false)
                .message("비밀번호 찾기 시스템에 일시적인 문제가 발생했습니다.")
                .build();
    }

    public AuthResponse resetPasswordFallback(ResetPasswordRequest request, Exception ex) {
        return AuthResponse.builder()
                .success(false)
                .message("비밀번호 재설정 시스템에 일시적인 문제가 발생했습니다.")
                .build();
    }

    public AuthResponse resetPasswordImmediateFallback(ImmediateResetRequest request, Exception ex) {
        return AuthResponse.builder()
                .success(false)
                .message("비밀번호 초기화 시스템에 일시적인 문제가 발생했습니다.")
                .build();
    }

    public boolean verifyUserInfoFallback(String userId, String email, String name, Exception ex) {
        return false;
    }

    public UserDto getUserFromUserServiceFallback(String userId, Exception ex) {
        return UserDto.builder()
                .userId(userId)
                .name("임시사용자")
                .email("temp@example.com")
                .status("TEMP")
                .build();
    }

    public boolean updatePasswordInUserServiceFallback(String userId, String newPassword, Exception ex) {
        return false;
    }
}