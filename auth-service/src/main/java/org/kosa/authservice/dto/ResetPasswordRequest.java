package org.kosa.authservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordRequest {
    @NotBlank(message = "아이디는 필수입니다")
    private String userid;

    @NotBlank(message = "인증번호는 필수입니다")
    private String verificationCode;

    @NotBlank(message = "새 비밀번호는 필수입니다")
    @Size(min = 8, max = 20, message = "비밀번호는 8~20자 사이여야 합니다")
    @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[!@#$%^&*])[A-Za-z\\d!@#$%^&*]+$",
            message = "비밀번호는 영문, 숫자, 특수문자를 포함해야 합니다")
    private String newPassword;

    @NotBlank(message = "비밀번호 확인은 필수입니다")
    private String confirmPassword;
}