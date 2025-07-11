package org.kosa.commerceservice.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentVerifyResponse {
    private boolean success;
    private String paymentId;
    private String orderId;
    private Integer amount;
    private PaymentStatus status;
    private String message;
}