package org.kosa.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {

    private boolean success;
    private String message;
    private String token;
    private Long userId;
    private String username;
    private String name;
    private String email;
    private String phone;

    // data 필드 추가 (비밀번호 찾기에서 마스킹된 이메일 등을 반환할 때 사용)
//    private Object data;
}