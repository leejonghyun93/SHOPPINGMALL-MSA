package org.kosa.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

// 2. 카프카 이벤트 DTO
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserWithdrawalEvent {
    private String eventId;
    private String userId;
    private String withdrawnId;
    private String name;
    private String email;
    private String phone;
    private String gradeId;
    private String withdrawalReason;
    private LocalDate originalCreatedDate;
    private LocalDate withdrawalDate;
    private LocalDate secessionDate;
    private LocalDateTime eventTimestamp;
    @Builder.Default
    private String eventType = "USER_WITHDRAWAL";
}