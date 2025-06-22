package org.kosa.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
// 1. 회원탈퇴 요청 DTO
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WithdrawResponseDto {
    private boolean success;
    private String message;
    private String withdrawnId;
}