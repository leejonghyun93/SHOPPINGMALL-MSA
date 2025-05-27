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
        UserDto user = userFeignClient.getUserByUserId(loginRequest.getUserid());

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "아이디 또는 비밀번호가 일치하지 않습니다."));
        }

        if (Boolean.TRUE.equals(user.getAccountLocked())) {
            return ResponseEntity.status(HttpStatus.LOCKED)  // 423 Locked
                    .body(Map.of("message", "계정이 잠겨 있습니다. 관리자에게 문의하세요."));
        }

        if (!passwordEncoder.matches(loginRequest.getPasswd(), user.getPasswd())) {
            try {
                userFeignClient.increaseLoginFailCount(loginRequest.getUserid());
            } catch (feign.FeignException e) {
                if (e.status() == 423) {
                    return ResponseEntity.status(HttpStatus.LOCKED)
                            .body(Map.of("message", "계정이 잠겨 더 이상 로그인 실패 횟수를 증가시킬 수 없습니다."));
                }
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("message", "로그인 실패 횟수 증가 중 오류가 발생했습니다."));
            }

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "아이디 또는 비밀번호가 일치하지 않습니다."));
        }

        // 로그인 성공 시 실패 횟수 초기화
        userFeignClient.resetLoginFailCount(loginRequest.getUserid());

        String token = jwtUtil.generateToken(user.getUserid(), user.getRole(), user.getFullAddress(), user.getLoginTime());
        return ResponseEntity.ok(new TokenResponse(token));
    }

}
