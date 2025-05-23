package org.kosa.authservice.controller;

import org.kosa.authservice.security.JwtUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    public AuthController(AuthenticationManager authenticationManager, JwtUtil jwtUtil) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public Map<String, String> login(@RequestBody Map<String, String> user) {
        try {
            String userid = user.get("userid");
            String passwd = user.get("password");

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(userid, passwd)
            );

            var principal = (org.springframework.security.core.userdetails.User) authentication.getPrincipal();

            String token = jwtUtil.generateToken(principal.getUsername(),
                    principal.getAuthorities().iterator().next().getAuthority());

            return Map.of("token", token);

        } catch (AuthenticationException e) {
            throw new RuntimeException("로그인 실패", e);
        }
    }
}