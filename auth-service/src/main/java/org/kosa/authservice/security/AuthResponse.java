package org.kosa.authservice.security;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private boolean success;
    private String message;
    private String token;
    private String userId;
    private String username;
    private String name;
    private String email;
    private String phone;
}
