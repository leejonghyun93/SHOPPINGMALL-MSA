package org.kosa.authservice.dto;

import lombok.Data;

@Data
public class UpdatePasswordRequest {
    private String newPassword; // 암호화된 비밀번호
}
