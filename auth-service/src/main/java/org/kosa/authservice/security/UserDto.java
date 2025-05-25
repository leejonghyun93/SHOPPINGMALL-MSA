package org.kosa.authservice.security;

import lombok.Data;

import java.util.List;

@Data
public class UserDto {
    private String userid;
    private String passwd;
    private String role;
    private String nickname;
    private List<String> roles;
}

