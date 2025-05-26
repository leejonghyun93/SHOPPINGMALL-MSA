package org.kosa.authservice.security;

import lombok.Data;

@Data
public class LoginRequest {
    private String userid;
    private String passwd;

}