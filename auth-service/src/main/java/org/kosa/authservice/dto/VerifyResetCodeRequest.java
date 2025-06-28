package org.kosa.authservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyResetCodeRequest {
    @NotBlank(message = "아이디는 필수입니다")
    private String userid;

    @NotBlank(message = "인증번호는 필수입니다")
    private String verificationCode;
}