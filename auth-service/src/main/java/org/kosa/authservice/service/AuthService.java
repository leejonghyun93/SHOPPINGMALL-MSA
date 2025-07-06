package org.kosa.authservice.service;

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
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    @Value("${user-service.url:http://localhost:8103}")
    private String userServiceUrl;

    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final RestTemplate restTemplate;

    private final Map<String, String> resetCodes = new ConcurrentHashMap<>();

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

            // 🔥 이름을 포함하여 토큰 생성
            String actualName = determineUserName(user);
            log.info("🔍 일반 로그인 토큰 생성 - userId: {}, name: '{}'", user.getUserId(), actualName);

            String accessToken = jwtUtil.generateToken(user.getUserId(), "USER", actualName);
            String refreshToken = jwtUtil.generateRefreshToken(user.getUserId());

            return AuthResponse.builder()
                    .success(true)
                    .message("로그인 성공")
                    .token(accessToken)
                    .userId(userId)
                    .username(user.getUserId())
                    .name(actualName) // 🔥 응답에도 이름 포함
                    .email(user.getEmail())
                    .phone(user.getPhone())
                    .build();

        } catch (Exception e) {
            log.error("로그인 처리 실패: userId={}, error={}", userId, e.getMessage(), e);
            return AuthResponse.builder()
                    .success(false)
                    .message("로그인 처리 중 오류가 발생했습니다.")
                    .build();
        }
    }

    /**
     * 🔥 사용자 이름 결정 로직
     */
    private String determineUserName(UserDto user) {
        // 1. DB에서 가져온 이름이 유효하면 사용
        if (user.getName() != null && !user.getName().trim().isEmpty() &&
                !user.getName().equals("사용자") && !user.getName().equals("소셜사용자")) {
            return user.getName().trim();
        }

        // 2. 이름이 없으면 userId를 이름으로 사용
        return user.getUserId();
    }

    private void cacheUserSessionInUserService(String userId) {
        try {
            String url = userServiceUrl + "/api/users/cache/" + userId;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.debug("User Service 캐시 저장 요청 성공: userId={}", userId);
            } else {
                log.warn("User Service 캐시 저장 요청 실패: userId={}, status={}",
                        userId, response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("User Service 캐시 저장 요청 실패: userId={}, error={}", userId, e.getMessage());
        }
    }

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

                log.debug("토큰 검증 성공, 사용자 정보 조회 시작: userId={}", userId);

                UserDto sessionUser = getUserFromUserServiceSession(userId);

                if (sessionUser != null) {
                    log.debug("세션에서 사용자 정보 조회 성공: userId={}", userId);
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

                log.debug("세션 조회 실패, DB에서 직접 조회: userId={}", userId);
                UserDto dbUser = getUserFromUserService(userId);

                if (dbUser != null) {
                    log.debug("DB에서 사용자 정보 조회 성공: userId={}", userId);
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
                    log.warn("사용자 정보를 찾을 수 없음: userId={}", userId);
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
            log.error("토큰 검증 실패: {}", e.getMessage());
            return AuthResponse.builder()
                    .success(false)
                    .message("토큰 검증 중 오류가 발생했습니다")
                    .build();
        }
    }

    private UserDto getUserFromUserServiceSession(String userId) {
        try {
            String url = userServiceUrl + "/api/users/session/" + userId;
            log.debug("세션 조회 API 호출: {}", url);

            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                Boolean success = (Boolean) responseBody.get("success");

                if (Boolean.TRUE.equals(success)) {
                    Map<String, Object> data = (Map<String, Object>) responseBody.get("data");

                    if (data != null) {
                        UserDto user = UserDto.builder()
                                .userId((String) data.get("userId"))
                                .name((String) data.get("name"))
                                .email((String) data.get("email"))
                                .phone((String) data.get("phone"))
                                .gradeId((String) data.get("gradeId"))
                                .status((String) data.get("status"))
                                .build();

                        log.debug("세션 데이터 파싱 성공: name={}, email={}, phone={}",
                                user.getName(), user.getEmail(), user.getPhone());
                        return user;
                    }
                }
            }

            log.debug("세션 조회 실패 또는 데이터 없음");
            return null;

        } catch (Exception e) {
            log.error("세션 조회 중 오류: userId={}, error={}", userId, e.getMessage());
            return null;
        }
    }

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
            log.error("토큰 갱신 실패: {}", e.getMessage());
            return AuthResponse.builder()
                    .success(false)
                    .message("토큰 갱신에 실패했습니다")
                    .build();
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
            log.error("로그아웃 처리 실패: {}", e.getMessage());
            return AuthResponse.builder()
                    .success(true)
                    .message("로그아웃되었습니다")
                    .build();
        }
    }

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
            log.error("사용자 정보 조회 실패: {}", e.getMessage());
            return AuthResponse.builder()
                    .success(false)
                    .message("사용자 정보 조회 중 오류가 발생했습니다")
                    .build();
        }
    }

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
            log.error("비밀번호 찾기 실패: {}", e.getMessage());
            return AuthResponse.builder()
                    .success(false)
                    .message("비밀번호 찾기 처리 중 오류가 발생했습니다.")
                    .build();
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
            log.error("인증번호 확인 실패: {}", e.getMessage());
            return AuthResponse.builder()
                    .success(false)
                    .message("인증번호 확인 중 오류가 발생했습니다.")
                    .build();
        }
    }

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
            log.error("비밀번호 재설정 실패: {}", e.getMessage());
            return AuthResponse.builder()
                    .success(false)
                    .message("비밀번호 재설정 중 오류가 발생했습니다.")
                    .build();
        }
    }

    private UserDto getUserFromUserService(String userId) {
        try {
            String url = userServiceUrl + "/api/users/" + userId;
            log.debug("User Service 호출: {}", url);

            ResponseEntity<UserDto> response = restTemplate.getForEntity(url, UserDto.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.debug("User Service 응답 성공");
                return response.getBody();
            } else {
                log.warn("User Service 응답 실패: {}", response.getStatusCode());
                return null;
            }
        } catch (Exception e) {
            log.error("User Service 호출 실패: {}", e.getMessage());
            return null;
        }
    }

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
            log.error("비밀번호 업데이트 요청 실패: {}", e.getMessage());
            return false;
        }
    }

    private String generateResetCode() {
        return String.format("%06d", (int) (Math.random() * 1000000));
    }
}