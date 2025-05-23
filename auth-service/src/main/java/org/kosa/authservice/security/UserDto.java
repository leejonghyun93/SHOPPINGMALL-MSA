package org.kosa.authservice.security;

import lombok.Data;

@Data
public class UserDto {
    private String userid;
    private String passwd;
    private String role;
}

