package org.kosa.livestreamingservice.dto;

import lombok.*;

import java.time.LocalDateTime;

// Payment 취소 응답 DTO
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentCancelResponse {
    private String paymentId;
    private String orderId;
    private PaymentStatus status;
    private Integer cancelAmount;
    private String cancelReason;
    private LocalDateTime cancelledAt;
}
