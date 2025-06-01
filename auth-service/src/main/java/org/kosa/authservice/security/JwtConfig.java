package org.kosa.authservice.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

@RefreshScope
@Component
public class JwtConfig {

    @Value("${jwt.secret-key}")
    private String secretKey;

    public String getSecretKey() {
        return secretKey;
    }
}