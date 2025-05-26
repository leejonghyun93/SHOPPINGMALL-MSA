package org.kosa.authservice.security;

import lombok.Data;

import java.util.List;

@Data
public class UserResponse {
    private String userid;
    private String passwd;
    private List<String> roles;
    private String nickname;
    private String username;
    private String password;
    // 필요한 필드를 user-service API 응답에 맞게 추가
}