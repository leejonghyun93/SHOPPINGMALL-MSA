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
    @PostMapping("/find-id")
    public ResponseEntity<?> findId(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String name = request.get("name");

        try {
            UserDto user = userFeignClient.getUserByNameAndEmail(name, email);
            if (user != null) {
                return ResponseEntity.ok(Map.of("userid", user.getUserid()));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "일치하는 회원 정보가 없습니다."));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "아이디 찾기 중 오류 발생"));
        }
    }
    @PostMapping("/find-password")
    public ResponseEntity<?> findPassword(@RequestBody Map<String, String> request) {
        String userid = request.get("userid");
        String email = request.get("email");

        try {
            UserDto user = userFeignClient.getUserByUserId(userid);

            if (user == null || !user.getEmail().equals(email)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "일치하는 회원 정보가 없습니다."));
            }

            // 임시 비밀번호 생성
            String tempPassword = "Temp" + ((int) (Math.random() * 100000)); // 보안적으로는 더 복잡하게
            String encodedTempPassword = passwordEncoder.encode(tempPassword);

            // 비밀번호 변경
            userFeignClient.updatePassword(userid, encodedTempPassword);

            // 이메일 발송 로직 (메일 서비스와 연동 필요)
            // mailService.sendTempPassword(email, tempPassword);

            return ResponseEntity.ok(Map.of("message", "임시 비밀번호가 이메일로 발송되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "비밀번호 찾기 중 오류 발생"));
        }
    }
}
