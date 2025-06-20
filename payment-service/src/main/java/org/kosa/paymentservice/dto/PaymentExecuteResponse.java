package org.kosa.paymentservice.dto;

import lombok.*;

import java.time.LocalDateTime;

// Payment 실행 응답 DTO
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentExecuteResponse {
    private String paymentId;
    private String orderId;
    private PaymentStatus status;
    private Integer amount;
    private LocalDateTime paidAt;
    private String approvalNumber;
    private String cardName;
    private String bankName;
}