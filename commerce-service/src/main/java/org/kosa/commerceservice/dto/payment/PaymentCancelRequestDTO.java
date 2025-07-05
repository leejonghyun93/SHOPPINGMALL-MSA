package org.kosa.commerceservice.dto.payment;

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
    private String orderId;
    private String userId;
}