package org.kosa.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private String userid;
    private String name;
    private String email;
    private int age;
    private String fullAddress;
    private String phone;
    private String nickname;
    private String role;
    private Boolean accountLocked;
    private LocalDateTime loginTime;
    private LocalDateTime regDate;


}