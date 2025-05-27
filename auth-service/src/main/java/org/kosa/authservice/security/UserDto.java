package org.kosa.authservice.security;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class UserDto {
    private String userid;
    private String passwd;
    private String role;
    private String nickname;
    private List<String> roles;

    private String fullAddress;
    private LocalDateTime loginTime;

    // 로그인 실패 횟수 추가
    private Integer loginFailCount;

    // 계정 잠금 여부 추가
    private Boolean accountLocked;
}

