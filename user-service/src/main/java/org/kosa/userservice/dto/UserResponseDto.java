package org.kosa.userservice.dto;

import lombok.*;
import org.kosa.userservice.entity.User;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponseDto {
    private String userid;
    private String passwd;
    private String name;
    private String email;
    private int age;
    private String role;
    private LocalDateTime regDate;
    private int loginFailCount;
    private String address;
    private String detailAddress;
    private String fullAddress;
    private String phone;
    private String nickname;
    private LocalDateTime loginTime;
    private Boolean accountLocked;


}
