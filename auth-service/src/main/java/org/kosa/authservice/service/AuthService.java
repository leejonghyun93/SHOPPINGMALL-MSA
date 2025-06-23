package org.kosa.authservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.kosa.authservice.dto.*;
import org.kosa.authservice.security.AuthResponse;
import org.kosa.authservice.security.JwtUtil;
import org.kosa.authservice.security.UserClient;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserClient userClient;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    /**
     * 로그인 처리 (User Service와 연동) - 🔥 문자열 userId 완벽 지원
     */
    public AuthResponse login(LoginRequest loginRequest) {
        try {
            log.info("🚀 로그인 요청 시작: {}", loginRequest.getUserid());

            UserDto user;
            try {
                user = userClient.getUserByUserId(loginRequest.getUserid());
                log.info("🔍 UserClient에서 받은 응답:");
                log.info("   - userId: '{}'", user.getUserId());
                log.info("   - status: '{}'", user.getStatus());
                log.info("   - secessionYn: '{}'", user.getSecessionYn());
                log.info("   - name: '{}'", user.getName());
            } catch (Exception feignException) {
                log.error("❌ 사용자 서비스 연결 실패: {}", feignException.getMessage());
                throw new IllegalArgumentException("사용자 서비스 연결 실패: " + feignException.getMessage());
            }

            if (user == null) {
                log.warn("❌ 사용자를 찾을 수 없음: {}", loginRequest.getUserid());
                throw new IllegalArgumentException("사용자를 찾을 수 없습니다");
            }

            // 🔥 탈퇴한 회원 체크 추가
            log.info("🔍 탈퇴 회원 체크 - status: '{}', secessionYn: '{}'", user.getStatus(), user.getSecessionYn());

            if ("Y".equals(user.getSecessionYn()) || "WITHDRAWN".equals(user.getStatus())) {
                log.warn("⚠️ 탈퇴한 회원 로그인 시도 차단: {}", loginRequest.getUserid());
                throw new IllegalArgumentException("탈퇴한 회원입니다. 로그인할 수 없습니다.");
            }

            // 🔥 비활성화된 회원 체크 추가
            if (!"ACTIVE".equals(user.getStatus())) {
                log.warn("⚠️ 비활성화된 회원 로그인 시도 차단: {}", loginRequest.getUserid());
                throw new IllegalArgumentException("비활성화된 계정입니다. 관리자에게 문의하세요.");
            }

            if (user.getPassword() == null || user.getPassword().isEmpty()) {
                log.warn("❌ 사용자의 비밀번호가 null 또는 empty: {}", loginRequest.getUserid());
                throw new IllegalArgumentException("비밀번호가 일치하지 않습니다");
            }

            boolean isValidPassword;
            try {
                isValidPassword = passwordEncoder.matches(
                        loginRequest.getPasswd(),
                        user.getPassword()
                );
                log.info("🔍 비밀번호 검증 결과: {} (사용자: {})", isValidPassword, loginRequest.getUserid());
            } catch (Exception pwException) {
                log.error("❌ 비밀번호 검증 실패: {}", pwException.getMessage());
                throw new IllegalArgumentException("비밀번호 검증 실패");
            }

            if (!isValidPassword) {
                log.warn("❌ 비밀번호 불일치: {}", loginRequest.getUserid());
                throw new IllegalArgumentException("비밀번호가 일치하지 않습니다");
            }

            try {
                // 🔥 중요: userId가 숫자인지 문자열인지 구분
                Long userIdLong = user.getUserIdAsLong(); // 숫자 변환 시도 (실패하면 null)
                String username = user.getUsername();     // userId와 동일
                String name = user.getName();
                String email = user.getEmail();
                String phone = user.getPhone();

                log.info("🎯 토큰 생성 준비:");
                log.info("   - userIdLong: {}", userIdLong);
                log.info("   - username: '{}'", username);
                log.info("   - hasNumericUserId: {}", userIdLong != null);
                log.info("   - hasStringUserId: {}", userIdLong == null);

                // 🔥 토큰 생성: userIdLong이 null이면 username을 subject로 사용
                String token = jwtUtil.generateToken(userIdLong, username, name, email, phone);

                log.info("✅ 로그인 성공 - userId: {}, username: '{}'", userIdLong, username);

                return AuthResponse.builder()
                        .success(true)
                        .message("로그인 성공")
                        .token(token)
                        .userId(userIdLong)  // 🔥 null일 수 있음 (문자열 userId인 경우)
                        .username(username)
                        .name(name)
                        .email(email)
                        .phone(phone)
                        .build();

            } catch (Exception tokenException) {
                log.error("❌ 토큰 생성 실패: {}", tokenException.getMessage(), tokenException);
                throw new IllegalArgumentException("토큰 생성 실패: " + tokenException.getMessage());
            }

        } catch (IllegalArgumentException e) {
            log.warn("⚠️ 로그인 실패: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("❌ 로그인 처리 중 예상치 못한 오류: {}", e.getMessage(), e);
            throw new IllegalArgumentException("로그인 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 토큰 검증 - 🔥 문자열 userId 완벽 지원
     */
    public AuthResponse validateToken(String token) {
        log.info("🔍 토큰 검증 요청");

        if (!jwtUtil.validateToken(token)) {
            log.warn("❌ 유효하지 않은 토큰");
            throw new IllegalArgumentException("유효하지 않은 토큰입니다");
        }

        String username = jwtUtil.getUsernameFromToken(token);
        String subject = jwtUtil.getSubjectFromToken(token);
        Long userId = jwtUtil.getUserIdFromToken(token); // 숫자인 경우만, 문자열이면 null

        log.info("🔍 토큰에서 추출된 정보:");
        log.info("   - subject: '{}'", subject);
        log.info("   - username: '{}'", username);
        log.info("   - userId (숫자): {}", userId);

        try {
            // username으로 사용자 조회
            UserDto user = userClient.getUserByUserId(username);
            if (user == null) {
                log.warn("❌ 토큰의 사용자를 DB에서 찾을 수 없음: {}", username);
                throw new IllegalArgumentException("유효하지 않은 사용자입니다");
            }

            log.info("✅ 토큰 검증 성공: {}", username);

            return AuthResponse.builder()
                    .success(true)
                    .message("토큰이 유효합니다")
                    .userId(userId)  // 🔥 null일 수 있음 (문자열 userId인 경우)
                    .username(username)
                    .build();

        } catch (Exception e) {
            log.error("❌ 사용자 검증 중 오류: {}", e.getMessage());
            throw new IllegalArgumentException("사용자 검증 중 오류가 발생했습니다");
        }
    }

    /**
     * 토큰 갱신 - 🔥 문자열 userId 완벽 지원 (만료된 토큰도 처리)
     */
    public AuthResponse refreshToken(String token) {
        try {
            log.info("🔄 토큰 갱신 요청 처리 시작");

            // 토큰에서 사용자 정보 추출 (만료되었어도 시도)
            String username = null;
            String subject = null;
            Long userId = null;
            String name = null;
            String email = null;
            String phone = null;

            try {
                // 만료되지 않은 경우
                if (!jwtUtil.isTokenExpired(token)) {
                    log.info("🔄 유효한 토큰에서 사용자 정보 추출");
                    username = jwtUtil.getUsernameFromToken(token);
                    subject = jwtUtil.getSubjectFromToken(token);
                    userId = jwtUtil.getUserIdFromToken(token);  // 🔥 null이 반환될 수 있음
                    name = jwtUtil.getNameFromToken(token);
                    email = jwtUtil.getEmailFromToken(token);
                    phone = jwtUtil.getPhoneFromToken(token);
                } else {
                    // 만료된 토큰에서도 정보 추출 시도
                    log.info("🔄 만료된 토큰에서 사용자 정보 추출 시도");
                    username = jwtUtil.getUsernameFromExpiredToken(token);
                    subject = jwtUtil.getSubjectFromExpiredToken(token);
                    userId = jwtUtil.getUserIdFromExpiredToken(token);  // 🔥 null이 반환될 수 있음
                    name = jwtUtil.getNameFromExpiredToken(token);
                    email = jwtUtil.getEmailFromExpiredToken(token);
                    phone = jwtUtil.getPhoneFromExpiredToken(token);
                }

                log.info("🔄 토큰에서 추출된 정보:");
                log.info("   - subject: '{}'", subject);
                log.info("   - username: '{}'", username);
                log.info("   - userId (숫자): {}", userId);

            } catch (Exception e) {
                log.error("❌ 토큰에서 사용자 정보 추출 실패: {}", e.getMessage());
                throw new IllegalArgumentException("토큰에서 사용자 정보를 추출할 수 없습니다");
            }

            // 🔥 username은 필수
            if (username == null || username.trim().isEmpty()) {
                log.error("❌ 토큰에서 사용자명을 찾을 수 없음");
                throw new IllegalArgumentException("토큰에서 사용자명을 찾을 수 없습니다");
            }

            log.info("🔄 토큰 갱신 요청 사용자: subject='{}', username='{}', userId={}", subject, username, userId);

            // 사용자가 여전히 유효한지 확인
            try {
                UserDto user = userClient.getUserByUserId(username);
                if (user == null) {
                    log.warn("❌ 토큰 갱신 시 사용자를 DB에서 찾을 수 없음: {}", username);
                    throw new IllegalArgumentException("유효하지 않은 사용자입니다");
                }

                // 🔥 DB에서 실제 userId 가져오기
                Long actualUserId = user.getUserIdAsLong();
                log.info("🔍 DB에서 가져온 실제 userId: {}", actualUserId);
                log.info("🔍 사용자 타입: {}", actualUserId != null ? "숫자 ID" : "문자열 ID");

                // 🔥 새 토큰 생성: actualUserId가 null이어도 username을 subject로 사용
                String newToken = jwtUtil.generateToken(
                        actualUserId,  // 🔥 null일 수 있음 - generateToken에서 처리됨
                        username,
                        name != null ? name : user.getName(),
                        email != null ? email : user.getEmail(),
                        phone != null ? phone : user.getPhone()
                );

                log.info("✅ 토큰 갱신 성공:");
                log.info("   - 새 userId: {}", actualUserId);
                log.info("   - username: '{}'", username);
                log.info("   - 새 토큰 길이: {}", newToken.length());

                return AuthResponse.builder()
                        .success(true)
                        .message("토큰이 갱신되었습니다")
                        .token(newToken)
                        .userId(actualUserId)  // 🔥 null일 수 있음 (문자열 userId인 경우)
                        .username(username)
                        .name(name != null ? name : user.getName())
                        .email(email != null ? email : user.getEmail())
                        .phone(phone != null ? phone : user.getPhone())
                        .build();

            } catch (Exception e) {
                log.error("❌ 사용자 검증 실패: {}", e.getMessage());
                throw new IllegalArgumentException("사용자 검증 중 오류가 발생했습니다: " + e.getMessage());
            }

        } catch (IllegalArgumentException e) {
            log.warn("⚠️ 토큰 갱신 실패: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("❌ 토큰 갱신 처리 중 예상치 못한 오류: {}", e.getMessage(), e);
            throw new IllegalArgumentException("토큰 갱신 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}