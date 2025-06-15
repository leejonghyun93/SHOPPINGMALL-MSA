package org.kosa.authservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.kosa.authservice.dto.*;
import org.kosa.authservice.security.AuthResponse;
import org.kosa.authservice.security.JwtUtil;
import org.kosa.authservice.security.UserClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserClient userClient;
    private final JwtUtil jwtUtil;

    /**
     * 로그인 처리 (User Service와 연동)
     */
    public AuthResponse login(LoginRequest loginRequest) {
        try {
            // User Service에서 사용자 정보 조회
            UserDto user = userClient.getUserByUsername(loginRequest.getUsername());
            if (user == null) {
                throw new IllegalArgumentException("사용자를 찾을 수 없습니다");
            }

            // 계정 상태 확인
            if (!"ACTIVE".equals(user.getStatus())) {
                throw new IllegalArgumentException("비활성화된 계정입니다");
            }

            // User Service에서 비밀번호 검증
            PasswordValidationRequest validationRequest = new PasswordValidationRequest(
                    loginRequest.getUsername(), loginRequest.getPassword());

            boolean isValidPassword = userClient.validatePassword(validationRequest);
            if (!isValidPassword) {
                throw new IllegalArgumentException("비밀번호가 일치하지 않습니다");
            }

            // JWT 토큰 생성
            String token = jwtUtil.generateToken(user.getUserId(), user.getUsername());

            return AuthResponse.builder()
                    .success(true)
                    .message("로그인 성공")
                    .token(token)
                    .userId(user.getUserId())
                    .username(user.getUsername())
                    .build();

        } catch (Exception e) {
            log.error("User Service 호출 실패: {}", e.getMessage());
            throw new IllegalArgumentException("로그인 처리 중 오류가 발생했습니다");
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
        Long userId = jwtUtil.getUserIdFromToken(token);

        try {
            // User Service에서 사용자 존재 여부 확인
            UserDto user = userClient.getUserByUsername(username);
            if (user == null || !"ACTIVE".equals(user.getStatus())) {
                throw new IllegalArgumentException("유효하지 않은 사용자입니다");
            }

            return AuthResponse.builder()
                    .success(true)
                    .message("토큰이 유효합니다")
                    .userId(userId)
                    .username(username)
                    .build();

        } catch (Exception e) {
            log.error("User Service 호출 실패: {}", e.getMessage());
            throw new IllegalArgumentException("사용자 검증 중 오류가 발생했습니다");
        }
    }

    /**
     * 토큰 갱신
     */
    public AuthResponse refreshToken(String token) {
        if (jwtUtil.isTokenExpired(token)) {
            throw new IllegalArgumentException("만료된 토큰입니다");
        }

        String username = jwtUtil.getUsernameFromToken(token);
        Long userId = jwtUtil.getUserIdFromToken(token);

        try {
            // User Service에서 사용자 존재 여부 확인
            UserDto user = userClient.getUserByUsername(username);
            if (user == null || !"ACTIVE".equals(user.getStatus())) {
                throw new IllegalArgumentException("유효하지 않은 사용자입니다");
            }

            // 새 토큰 생성
            String newToken = jwtUtil.generateToken(userId, username);

            return AuthResponse.builder()
                    .success(true)
                    .message("토큰이 갱신되었습니다")
                    .token(newToken)
                    .userId(userId)
                    .username(username)
                    .build();

        } catch (Exception e) {
            log.error("User Service 호출 실패: {}", e.getMessage());
            throw new IllegalArgumentException("토큰 갱신 중 오류가 발생했습니다");
        }
    }
}