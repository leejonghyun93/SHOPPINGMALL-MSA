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

                String token = jwtUtil.generateToken(userIdLong, username);

                return AuthResponse.builder()
                        .success(true)
                        .message("로그인 성공")
                        .token(token)
                        .userId(userIdLong)
                        .username(username)
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

    public AuthResponse refreshToken(String token) {
        if (jwtUtil.isTokenExpired(token)) {
            throw new IllegalArgumentException("만료된 토큰입니다");
        }

        String username = jwtUtil.getUsernameFromToken(token);
        Long userId = jwtUtil.getUserIdFromToken(token);

        try {
            UserDto user = userClient.getUserByUserId(username);
            if (user == null) {
                throw new IllegalArgumentException("유효하지 않은 사용자입니다");
            }

            String newToken = jwtUtil.generateToken(userId, username);

            return AuthResponse.builder()
                    .success(true)
                    .message("토큰이 갱신되었습니다")
                    .token(newToken)
                    .userId(userId)
                    .username(username)
                    .build();

        } catch (Exception e) {
            throw new IllegalArgumentException("토큰 갱신 중 오류가 발생했습니다");
        }
    }
}