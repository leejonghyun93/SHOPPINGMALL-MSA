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
     * 로그인 처리 (User Service와 연동)
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
                Long userIdLong = user.getUserIdAsLong();
                String username = user.getUsername();
                String name = user.getName();
                String email = user.getEmail();
                String phone = user.getPhone();

                String token = jwtUtil.generateToken(userIdLong, username, name,email,phone);

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
                throw new IllegalArgumentException("토큰 생성 실패");
            }

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("로그인 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    public AuthResponse validateToken(String token) {
        if (!jwtUtil.validateToken(token)) {
            throw new IllegalArgumentException("유효하지 않은 토큰입니다");
        }

        String username = jwtUtil.getUsernameFromToken(token);
        Long userId = jwtUtil.getUserIdFromToken(token);

        try {
            UserDto user = userClient.getUserByUserId(username);
            if (user == null) {
                throw new IllegalArgumentException("유효하지 않은 사용자입니다");
            }

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
     * 토큰 갱신 - 만료된 토큰도 처리 가능하도록 수정
     */
    public AuthResponse refreshToken(String token) {
        try {
            log.info("토큰 갱신 요청 처리 시작");

            // 토큰에서 사용자 정보 추출 (만료되었어도 시도)
            String username = null;
            Long userId = null;
            String name = null;
            String email = null;    // 🔥 email 추가
            String phone = null;    // 🔥 phone 추가

            try {
                // 만료되지 않은 경우
                if (!jwtUtil.isTokenExpired(token)) {
                    username = jwtUtil.getUsernameFromToken(token);
                    userId = jwtUtil.getUserIdFromToken(token);
                    name = jwtUtil.getNameFromToken(token);
                    email = jwtUtil.getEmailFromToken(token);     // 🔥 email 추출
                    phone = jwtUtil.getPhoneFromToken(token);     // 🔥 phone 추출
                } else {
                    // 만료된 토큰에서도 정보 추출 시도
                    log.info("만료된 토큰에서 사용자 정보 추출 시도");
                    username = jwtUtil.getUsernameFromExpiredToken(token);
                    userId = jwtUtil.getUserIdFromExpiredToken(token);
                    name = jwtUtil.getNameFromExpiredToken(token);
                    email = jwtUtil.getEmailFromExpiredToken(token);   // 🔥 만료된 토큰에서 email 추출
                    phone = jwtUtil.getPhoneFromExpiredToken(token);   // 🔥 만료된 토큰에서 phone 추출
                }
            } catch (Exception e) {
                log.error("토큰에서 사용자 정보 추출 실패: {}", e.getMessage());
                throw new IllegalArgumentException("토큰에서 사용자 정보를 추출할 수 없습니다");
            }

            if (username == null || userId == null) {
                throw new IllegalArgumentException("토큰에서 사용자 정보를 찾을 수 없습니다");
            }

            log.info("토큰 갱신 요청 사용자: userId={}, username={}", userId, username);

            // 사용자가 여전히 유효한지 확인
            try {
                UserDto user = userClient.getUserByUserId(username);
                if (user == null) {
                    throw new IllegalArgumentException("유효하지 않은 사용자입니다");
                }

                // 🔥 새 토큰 생성 시 email, phone 포함
                String newToken = jwtUtil.generateToken(
                        userId,
                        username,
                        name != null ? name : user.getName(),
                        email != null ? email : user.getEmail(),     // 토큰에서 추출한 email 또는 DB에서 가져온 email
                        phone != null ? phone : user.getPhone()      // 토큰에서 추출한 phone 또는 DB에서 가져온 phone
                );

                log.info("토큰 갱신 성공: userId={}", userId);

                return AuthResponse.builder()
                        .success(true)
                        .message("토큰이 갱신되었습니다")
                        .token(newToken)
                        .userId(userId)
                        .username(username)
                        .name(name != null ? name : user.getName())
                        .email(email != null ? email : user.getEmail())     // 🔥 응답에 email 추가
                        .phone(phone != null ? phone : user.getPhone())     // 🔥 응답에 phone 추가
                        .build();

            } catch (Exception e) {
                log.error("사용자 검증 실패: {}", e.getMessage());
                throw new IllegalArgumentException("사용자 검증 중 오류가 발생했습니다: " + e.getMessage());
            }

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("토큰 갱신 처리 중 예상치 못한 오류: {}", e.getMessage(), e);
            throw new IllegalArgumentException("토큰 갱신 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}