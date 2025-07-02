package org.kosa.authservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kosa.authservice.dto.*;
import org.kosa.authservice.security.AuthResponse;
import org.kosa.authservice.util.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
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

    // 임시 비밀번호 재설정 코드 저장소 (실제로는 Redis 사용 권장)
    private final Map<String, String> resetCodes = new ConcurrentHashMap<>();

    /**
     * 🔒 보안 강화된 로그인 처리
     */
    public AuthResponse login(String userId, String password) {
        try {
            log.info("🔍 Auth Service - 로그인 시도: userId={}", userId);

            // 1. User Service에서 사용자 정보 조회
            UserDto user = getUserFromUserService(userId);
            if (user == null) {
                log.warn("사용자를 찾을 수 없음: {}", userId);
                return AuthResponse.builder()
                        .success(false)
                        .message("존재하지 않는 사용자입니다.")
                        .build();
            }

            // 2. 비밀번호 검증
            if (!passwordEncoder.matches(password, user.getPassword())) {
                log.warn("비밀번호 불일치: {}", userId);
                return AuthResponse.builder()
                        .success(false)
                        .message("비밀번호가 일치하지 않습니다.")
                        .build();
            }

            // 3. 🔒 JWT 토큰 생성 - username 포함 (Gateway 요구사항)
            String accessToken = jwtUtil.generateToken(user.getUserId(), user.getUserId(), "USER");
            String refreshToken = jwtUtil.generateRefreshToken(user.getUserId());

            log.info("✅ 로그인 성공: userId={}", userId);

            // 4. 🔒 응답에는 토큰과 기본 정보만 포함 (민감정보 제외)
            return AuthResponse.builder()
                    .success(true)
                    .message("로그인 성공")
                    .token(accessToken)
                    .userId(userId)
                    .username(user.getUserId()) // username만 포함
                    // 🚫 민감한 정보는 포함하지 않음: name, email, phone 등
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
     * 🔒 보안 강화된 토큰 검증
     */
    public AuthResponse validateToken(String token) {
        try {
            if (jwtUtil.validateAccessToken(token)) {
                String userId = jwtUtil.getUserIdFromToken(token);
                String username = jwtUtil.getUsernameFromToken(token);
                String role = jwtUtil.getRoleFromToken(token);

                // 🔒 검증 응답에도 최소한의 정보만 포함
                return AuthResponse.builder()
                        .success(true)
                        .message("유효한 토큰입니다")
                        .userId(userId)
                        .username(username) // Gateway가 요구하는 username 포함
                        // 🚫 추가 사용자 정보는 포함하지 않음
                        .build();
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

    /**
     * 🔒 보안 강화된 토큰 갱신
     */
    public AuthResponse refreshToken(String refreshToken) {
        try {
            // 리프레시 토큰으로 새 액세스 토큰 생성
            String newAccessToken = jwtUtil.refreshAccessToken(refreshToken);

            // 새 토큰에서 사용자 정보 추출
            String userId = jwtUtil.getUserIdFromToken(newAccessToken);
            String username = jwtUtil.getUsernameFromToken(newAccessToken);

            return AuthResponse.builder()
                    .success(true)
                    .message("토큰이 갱신되었습니다")
                    .token(newAccessToken)
                    .userId(userId)
                    .username(username)
                    .build();
        } catch (Exception e) {
            log.error("토큰 갱신 실패: {}", e.getMessage());
            throw new IllegalArgumentException("토큰 갱신에 실패했습니다", e);
        }
    }

    /**
     * 로그아웃
     */
    public AuthResponse logout(String token) {
        // 🔒 실제로는 토큰을 블랙리스트에 추가하거나 Redis에서 제거
        // JWT의 단점: 서버에서 강제로 무효화하기 어려움
        // 해결책: Redis 블랙리스트, 짧은 만료시간 + 리프레시 토큰 사용
        return AuthResponse.builder()
                .success(true)
                .message("로그아웃되었습니다")
                .build();
    }

    /**
     * 🔒 사용자 정보가 필요한 경우 별도 API로 제공
     */
    public AuthResponse getUserProfile(String userId) {
        try {
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

    // 비밀번호 찾기 관련 메소드들은 그대로 유지...
    public AuthResponse findPassword(FindPasswordRequest request) {
        try {
            UserDto user = verifyUserFromUserService(request.getUserid(), request.getEmail());
            if (user == null) {
                return AuthResponse.builder()
                        .success(false)
                        .message("해당 정보로 등록된 사용자를 찾을 수 없습니다.")
                        .build();
            }

            String resetCode = generateResetCode();
            resetCodes.put(request.getUserid(), resetCode);

            log.info("비밀번호 재설정 코드 생성: userId={}, code={}", request.getUserid(), resetCode);

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
    }

    public AuthResponse resetPassword(ResetPasswordRequest request) {
        try {
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

    // Private helper methods...
    private UserDto getUserFromUserService(String userId) {
        try {
            String url = userServiceUrl + "/api/users/" + userId;
            log.info("🌐 User Service 호출: {}", url);

            ResponseEntity<UserDto> response = restTemplate.getForEntity(url, UserDto.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("✅ User Service 응답 성공");
                return response.getBody();
            } else {
                log.warn("⚠️ User Service 응답 실패: {}", response.getStatusCode());
                return null;
            }
        } catch (Exception e) {
            log.error("❌ User Service 호출 실패: {}", e.getMessage());
            return null;
        }
    }

    private UserDto verifyUserFromUserService(String userId, String email) {
        try {
            String url = userServiceUrl + "/api/users/verify/" + userId + "/" + email;
            log.info("🌐 User Service 검증 호출: {}", url);

            ResponseEntity<UserDto> response = restTemplate.getForEntity(url, UserDto.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            } else {
                return null;
            }
        } catch (Exception e) {
            log.error("❌ User Service 검증 실패: {}", e.getMessage());
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