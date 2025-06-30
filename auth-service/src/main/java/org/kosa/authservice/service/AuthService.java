package org.kosa.authservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.kosa.authservice.dto.*;
import org.kosa.authservice.security.AuthResponse;
import org.kosa.authservice.security.JwtUtil;
import org.kosa.authservice.security.UserClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestTemplate;

import java.security.SecureRandom;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    @Value("${user-service.url}")
    private String userServiceUrl;

    private final RestTemplate restTemplate;
    private final EmailService emailService; // 이메일 발송 서비스
    private final RedisService redisService; // Redis 서비스 (인증번호 저장용)

    private static final String CHARACTERS = "0123456789";
    private static final int CODE_LENGTH = 6;
    private static final int CODE_EXPIRY_MINUTES = 10; // 10분 유효

    private final UserClient userClient;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    /**
     * 로그인 처리 (User Service와 연동) -  문자열 userId 완벽 지원
     */
    public AuthResponse login(LoginRequest loginRequest) {
        try {
            UserDto user;
            try {
                user = userClient.getUserByUserId(loginRequest.getUserid());
            } catch (Exception feignException) {
                throw new IllegalArgumentException("사용자 서비스 연결 실패: " + feignException.getMessage());
            }

            if (user == null) {
                throw new IllegalArgumentException("사용자를 찾을 수 없습니다");
            }
            if ("Y".equals(user.getSecessionYn()) || "WITHDRAWN".equals(user.getStatus())) {
                throw new IllegalArgumentException("탈퇴한 회원입니다. 로그인할 수 없습니다.");
            }

            // 비활성화된 회원 체크 추가
            if (!"ACTIVE".equals(user.getStatus())) {
                throw new IllegalArgumentException("비활성화된 계정입니다. 관리자에게 문의하세요.");
            }

            if (user.getPassword() == null || user.getPassword().isEmpty()) {
                throw new IllegalArgumentException("비밀번호가 일치하지 않습니다");
            }

            boolean isValidPassword;
            try {
                isValidPassword = passwordEncoder.matches(
                        loginRequest.getPasswd(),
                        user.getPassword()
                );
            } catch (Exception pwException) {
                throw new IllegalArgumentException("비밀번호 검증 실패");
            }
            if (!isValidPassword) {
                throw new IllegalArgumentException("비밀번호가 일치하지 않습니다");
            }

            try {
                String userIdString = user.getUserIdAsLong(); // String 반환 (메서드명은 기존 유지)
                String username = user.getUsername();         // userId와 동일
                String name = user.getName();
                String email = user.getEmail();
                String phone = user.getPhone();

                // 토큰 생성: userIdString이 null이면 username을 subject로 사용
                String token = jwtUtil.generateToken(userIdString, username, name, email, phone);

                return AuthResponse.builder()
                        .success(true)
                        .message("로그인 성공")
                        .token(token)
                        .userId(userIdString)  // String으로 반환
                        .username(username)
                        .name(name)
                        .email(email)
                        .phone(phone)
                        .build();

            } catch (Exception tokenException) {
                throw new IllegalArgumentException("토큰 생성 실패: " + tokenException.getMessage());
            }

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("로그인 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 토큰 검증
     */
    public AuthResponse validateToken(String token) {

        if (!jwtUtil.validateToken(token)) {
            throw new IllegalArgumentException("유효하지 않은 토큰입니다");
        }

        String username = jwtUtil.getUsernameFromToken(token);
        String subject = jwtUtil.getSubjectFromToken(token);
        String userId = jwtUtil.getUserIdFromToken(token);  // String 반환

        try {
            UserDto user = userClient.getUserByUserId(username);
            if (user == null) {
                throw new IllegalArgumentException("유효하지 않은 사용자입니다");
            }

            log.info("토큰 검증 성공: {}", username);

            return AuthResponse.builder()
                    .success(true)
                    .message("토큰이 유효합니다")
                    .userId(userId)    // String 타입
                    .username(username)
                    .build();

        } catch (Exception e) {
            throw new IllegalArgumentException("사용자 검증 중 오류가 발생했습니다");
        }
    }

    /**
     * 토큰 갱신
     */
    public AuthResponse refreshToken(String token) {
        try {

            String username = null;
            String subject = null;
            String userId = null;    // String 타입으로 변경
            String name = null;
            String email = null;
            String phone = null;

            try {
                // 만료되지 않은 경우
                if (!jwtUtil.isTokenExpired(token)) {
                    username = jwtUtil.getUsernameFromToken(token);
                    subject = jwtUtil.getSubjectFromToken(token);
                    userId = jwtUtil.getUserIdFromToken(token);      // String 반환
                    name = jwtUtil.getNameFromToken(token);
                    email = jwtUtil.getEmailFromToken(token);
                    phone = jwtUtil.getPhoneFromToken(token);
                } else {
                    username = jwtUtil.getUsernameFromExpiredToken(token);
                    subject = jwtUtil.getSubjectFromExpiredToken(token);
                    userId = jwtUtil.getUserIdFromExpiredToken(token); // String 반환
                    name = jwtUtil.getNameFromExpiredToken(token);
                    email = jwtUtil.getEmailFromExpiredToken(token);
                    phone = jwtUtil.getPhoneFromExpiredToken(token);
                }
            } catch (Exception e) {
                throw new IllegalArgumentException("토큰에서 사용자 정보를 추출할 수 없습니다");
            }

            if (username == null || username.trim().isEmpty()) {
                throw new IllegalArgumentException("토큰에서 사용자명을 찾을 수 없습니다");
            }
            // 사용자가 여전히 유효한지 확인
            try {
                UserDto user = userClient.getUserByUserId(username);
                if (user == null) {
                    throw new IllegalArgumentException("유효하지 않은 사용자입니다");
                }

                String actualUserId = user.getUserIdAsLong(); // String 반환

                String newToken = jwtUtil.generateToken(
                        actualUserId,  // String 타입
                        username,
                        name != null ? name : user.getName(),
                        email != null ? email : user.getEmail(),
                        phone != null ? phone : user.getPhone()
                );

                return AuthResponse.builder()
                        .success(true)
                        .message("토큰이 갱신되었습니다")
                        .token(newToken)
                        .userId(actualUserId)  // String 타입
                        .username(username)
                        .name(name != null ? name : user.getName())
                        .email(email != null ? email : user.getEmail())
                        .phone(phone != null ? phone : user.getPhone())
                        .build();

            } catch (Exception e) {
                throw new IllegalArgumentException("사용자 검증 중 오류가 발생했습니다: " + e.getMessage());
            }

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("토큰 갱신 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 비밀번호 찾기 - 아이디와 이메일 검증 후 인증번호 발송
     */
    public AuthResponse findPassword(FindPasswordRequest request) {
        log.info("비밀번호 찾기 처리 시작: 아이디={}", request.getUserid());

        try {
            // 1. User Service에서 사용자 정보 조회
            UserDto user = getUserByIdAndEmail(request.getUserid(), request.getEmail());

            if (user == null) {
                log.warn("사용자를 찾을 수 없음: 아이디={}, 이메일={}", request.getUserid(), request.getEmail());
                return AuthResponse.builder()
                        .success(false)
                        .message("입력하신 정보와 일치하는 계정을 찾을 수 없습니다.")
                        .build();
            }

            // 2. 계정 상태 확인
            if ("INACTIVE".equals(user.getStatus()) || "SUSPENDED".equals(user.getStatus())) {
                log.warn("비활성화된 계정: 아이디={}, 상태={}", request.getUserid(), user.getStatus());
                return AuthResponse.builder()
                        .success(false)
                        .message("비활성화된 계정입니다. 고객센터에 문의해주세요.")
                        .build();
            }

            // 3. 인증번호 생성
            String verificationCode = generateVerificationCode();
            log.info("인증번호 생성 완료: 아이디={}", request.getUserid());

            // 4. Redis에 인증번호 저장 (키: FIND_PWD:{userId}, 유효시간: 10분)
            String redisKey = "FIND_PWD:" + request.getUserid();
            redisService.setValueWithExpiry(redisKey, verificationCode, CODE_EXPIRY_MINUTES * 60);

            // 5. 이메일 발송
            boolean emailSent = sendVerificationEmail(user.getEmail(), user.getName(), verificationCode);

            if (!emailSent) {
                log.error("이메일 발송 실패: 아이디={}, 이메일={}", request.getUserid(), request.getEmail());
                return AuthResponse.builder()
                        .success(false)
                        .message("이메일 발송에 실패했습니다. 잠시 후 다시 시도해주세요.")
                        .build();
            }

            log.info("비밀번호 찾기 처리 완료: 아이디={}, 이메일={}", request.getUserid(), request.getEmail());

            return AuthResponse.builder()
                    .success(true)
                    .message("인증번호가 이메일로 발송되었습니다. 10분 내에 입력해주세요.")
                    .build();

        } catch (Exception e) {
            log.error("비밀번호 찾기 처리 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("비밀번호 찾기 처리 중 오류가 발생했습니다.");
        }
    }

    /**
     * User Service에서 아이디와 이메일로 사용자 조회 - MSA 환경용
     */
    private UserDto getUserByIdAndEmail(String userid, String email) {
        try {
            log.info("🔍 MSA - UserClient로 사용자 조회: userid={}, email={}", userid, email);

            // 1. UserClient를 사용해서 사용자 조회 (Feign)
            UserDto user = userClient.getUserByUserId(userid);

            if (user != null) {
                log.info("✅ 사용자 조회 성공: userId={}, userEmail={}", user.getUserId(), user.getEmail());

                // 2. 이메일 확인
                if (email != null && email.equals(user.getEmail())) {
                    log.info("✅ 이메일 일치 확인 완료");
                    return user;
                } else {
                    log.warn("⚠️ 이메일 불일치: 요청={}, 실제={}", email, user.getEmail());
                    return null;
                }
            } else {
                log.warn("⚠️ 사용자를 찾을 수 없음: userid={}", userid);
                return null;
            }

        } catch (feign.FeignException e) {
            log.error("❌ Feign 호출 실패: status={}, message={}", e.status(), e.getMessage());
            if (e.status() == 404) {
                log.info("👤 사용자 없음: userid={}", userid);
            }
            return null;
        } catch (Exception e) {
            log.error("❌ 사용자 조회 실패: userid={}, email={}, error={}", userid, email, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 6자리 숫자 인증번호 생성
     */
    private String generateVerificationCode() {
        Random random = new SecureRandom();
        StringBuilder code = new StringBuilder();

        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }

        return code.toString();
    }

    /**
     * 인증번호 이메일 발송
     */
    private boolean sendVerificationEmail(String email, String name, String verificationCode) {
        try {
            String subject = "[서비스명] 비밀번호 재설정 인증번호";
            String content = buildEmailContent(name, verificationCode);

            return emailService.sendEmail(email, subject, content);
        } catch (Exception e) {
            log.error("인증번호 이메일 발송 실패: email={}, error={}", email, e.getMessage());
            return false;
        }
    }

    /**
     * 이메일 내용 생성
     */
    private String buildEmailContent(String name, String verificationCode) {
        return String.format("""
                안녕하세요, %s님.
                
                비밀번호 재설정을 위한 인증번호를 안내드립니다.
                
                ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                
                인증번호: %s
                
                ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                
                ※ 주의사항
                - 인증번호는 10분간 유효합니다.
                - 인증번호를 타인에게 알려주지 마세요.
                - 본인이 요청하지 않았다면 즉시 고객센터(1588-1234)로 연락해주세요.
                
                감사합니다.
                """, name, verificationCode);
    }

    /**
     * 인증번호 검증
     */
    public AuthResponse verifyResetCode(VerifyResetCodeRequest request) {
        String redisKey = "FIND_PWD:" + request.getUserid();
        String storedCode = redisService.getValue(redisKey);

        if (storedCode == null) {
            return AuthResponse.builder()
                    .success(false)
                    .message("인증번호가 만료되었거나 존재하지 않습니다.")
                    .build();
        }

        if (!storedCode.equals(request.getVerificationCode())) {
            return AuthResponse.builder()
                    .success(false)
                    .message("인증번호가 일치하지 않습니다.")
                    .build();
        }

        // 인증번호 검증용 임시 토큰 생성 (5분 유효)
        String tempToken = generateTempToken(request.getUserid());
        redisService.setValueWithExpiry("TEMP_TOKEN:" + request.getUserid(), tempToken, 5 * 60);

        return AuthResponse.builder()
                .success(true)
                .message("인증번호가 확인되었습니다.")
                .token(tempToken)
                .build();
    }

    /**
     * 비밀번호 재설정
     */
    public AuthResponse resetPassword(ResetPasswordRequest request) {
        // 1. 비밀번호 확인 검증
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            return AuthResponse.builder()
                    .success(false)
                    .message("비밀번호가 일치하지 않습니다.")
                    .build();
        }

        // 2. 인증번호 재검증 (보안 강화)
        String redisKey = "FIND_PWD:" + request.getUserid();
        String storedCode = redisService.getValue(redisKey);

        if (storedCode == null || !storedCode.equals(request.getVerificationCode())) {
            return AuthResponse.builder()
                    .success(false)
                    .message("인증번호가 만료되었거나 일치하지 않습니다.")
                    .build();
        }

        try {
            // 3. User Service에 비밀번호 업데이트 요청
            boolean updated = updateUserPassword(request.getUserid(), request.getNewPassword());

            if (!updated) {
                return AuthResponse.builder()
                        .success(false)
                        .message("비밀번호 업데이트에 실패했습니다.")
                        .build();
            }

            // 4. Redis에서 인증번호 삭제
            redisService.deleteKey(redisKey);
            redisService.deleteKey("TEMP_TOKEN:" + request.getUserid());

            log.info("비밀번호 재설정 완료: 아이디={}", request.getUserid());

            return AuthResponse.builder()
                    .success(true)
                    .message("비밀번호가 성공적으로 변경되었습니다.")
                    .build();

        } catch (Exception e) {
            log.error("비밀번호 재설정 중 오류: {}", e.getMessage(), e);
            throw new RuntimeException("비밀번호 재설정 실패");
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
            updateRequest.setNewPassword(passwordEncoder.encode(newPassword)); // 암호화해서 전송

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
}