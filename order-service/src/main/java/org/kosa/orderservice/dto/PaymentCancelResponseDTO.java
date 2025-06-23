package org.kosa.orderservice.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCancelResponseDTO {
    private boolean success;
    private String cancelId;
    private String message;
    private String errorCode;
    private Integer refundAmount;
    private LocalDateTime cancelDate;
    private String paymentId;
}