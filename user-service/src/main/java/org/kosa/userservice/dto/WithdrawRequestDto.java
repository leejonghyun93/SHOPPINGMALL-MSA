package org.kosa.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;


//회원탈퇴 요청 DTO
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WithdrawRequestDto {
    private String userId;
    private String password;
    private String withdrawalReason;
    private LocalDate withdrawalDate;
}