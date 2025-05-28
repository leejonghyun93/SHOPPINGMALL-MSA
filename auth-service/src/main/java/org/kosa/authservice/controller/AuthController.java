package org.kosa.authservice.controller;


import org.kosa.authservice.security.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;

    private final UserFeignClient userFeignClient;
    private final JwtUtil jwtUtil;

    private final PasswordEncoder passwordEncoder;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtUtil jwtUtil,
                          UserFeignClient userFeignClient,
                          PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userFeignClient = userFeignClient;
        this.passwordEncoder = passwordEncoder;
    }


    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        UserDto user;
        try {
            user = userFeignClient.getUserByUserId(loginRequest.getUserid());
        } catch (Exception e) {
//            log.error("Feign 통신 오류 - userid: {}, message: {}", loginRequest.getUserid(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "유저 정보를 가져오는 중 오류 발생"));
        }

        // 유저 정보가 없을 경우
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "아이디 또는 비밀번호가 일치하지 않습니다."));
        }

        // 계정 잠금 여부 확인
        if (Boolean.TRUE.equals(user.getAccountLocked())) {
            return ResponseEntity.status(HttpStatus.LOCKED)
                    .body(Map.of("message", "계정이 잠겨 있습니다. 관리자에게 문의하세요."));
        }

        // 비밀번호 검증 실패 시 처리
        if (!passwordEncoder.matches(loginRequest.getPasswd(), user.getPasswd())) {
            int failCount = user.getLoginFailCount();

            try {
                // 실패 횟수 증가 요청
                userFeignClient.increaseLoginFailCount(loginRequest.getUserid());
                failCount += 1;

                if (failCount >= 5) {
                    return ResponseEntity.status(HttpStatus.LOCKED)
                            .body(Map.of("message", "5회 실패하셔서 계정이 잠금되었습니다."));
                } else {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body(Map.of("message", failCount + "회 실패하셨습니다."));
                }

            } catch (feign.FeignException e) {
                if (e.status() == 423) {
                    return ResponseEntity.status(HttpStatus.LOCKED)
                            .body(Map.of("message", "계정이 잠겨 더 이상 로그인 시도할 수 없습니다."));
                }

                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("message", "로그인 실패 횟수 처리 중 오류가 발생했습니다."));
            }
        }

        // 로그인 성공 시 실패 횟수 초기화
        userFeignClient.resetLoginFailCount(loginRequest.getUserid());

        // JWT 토큰 생성
        String token = jwtUtil.generateToken(
                user.getUserid(),
                user.getRole(),
                user.getFullAddress(),
                user.getLoginTime()
        );

        return ResponseEntity.ok(new TokenResponse(token));
    }

}
