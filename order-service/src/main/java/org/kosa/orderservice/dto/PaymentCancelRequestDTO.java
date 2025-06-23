package org.kosa.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCancelRequestDTO {
    private String paymentId;
    private Integer refundAmount;
    private String cancelReason;
    private String orderId;  // 추가 정보
    private String userId;   // 추가 정보
}