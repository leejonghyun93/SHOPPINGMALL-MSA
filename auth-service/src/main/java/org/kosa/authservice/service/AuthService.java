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
                Long userIdLong = user.getUserIdAsLong(); // 숫자 변환 시도 (실패하면 null)
                String username = user.getUsername();     // userId와 동일
                String name = user.getName();
                String email = user.getEmail();
                String phone = user.getPhone();

                // 토큰 생성: userIdLong이 null이면 username을 subject로 사용
                String token = jwtUtil.generateToken(userIdLong, username, name, email, phone);

                return AuthResponse.builder()
                        .success(true)
                        .message("로그인 성공")
                        .token(token)
                        .userId(userIdLong)
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
        Long userId = jwtUtil.getUserIdFromToken(token);


        try {
            UserDto user = userClient.getUserByUserId(username);
            if (user == null) {
                throw new IllegalArgumentException("유효하지 않은 사용자입니다");
            }

            log.info("토큰 검증 성공: {}", username);

            return AuthResponse.builder()
                    .success(true)
                    .message("토큰이 유효합니다")
                    .userId(userId)
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
            Long userId = null;
            String name = null;
            String email = null;
            String phone = null;

            try {
                // 만료되지 않은 경우
                if (!jwtUtil.isTokenExpired(token)) {
                    username = jwtUtil.getUsernameFromToken(token);
                    subject = jwtUtil.getSubjectFromToken(token);
                    userId = jwtUtil.getUserIdFromToken(token);
                    name = jwtUtil.getNameFromToken(token);
                    email = jwtUtil.getEmailFromToken(token);
                    phone = jwtUtil.getPhoneFromToken(token);
                } else {
                    username = jwtUtil.getUsernameFromExpiredToken(token);
                    subject = jwtUtil.getSubjectFromExpiredToken(token);
                    userId = jwtUtil.getUserIdFromExpiredToken(token);
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

                Long actualUserId = user.getUserIdAsLong();

                String newToken = jwtUtil.generateToken(
                        actualUserId,
                        username,
                        name != null ? name : user.getName(),
                        email != null ? email : user.getEmail(),
                        phone != null ? phone : user.getPhone()
                );

                return AuthResponse.builder()
                        .success(true)
                        .message("토큰이 갱신되었습니다")
                        .token(newToken)
                        .userId(actualUserId)
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
}