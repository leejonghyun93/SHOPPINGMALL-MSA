package org.kosa.authservice.security;

import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class UserDto {
    private String userid;
    private String passwd;
    private String role;
    private String nickname;
    private List<String> roles;
    private String fullAddress;
    private LocalDateTime loginTime;
    private Integer loginFailCount = 0;
    private Boolean accountLocked;
    private String email;
    private String name;
}