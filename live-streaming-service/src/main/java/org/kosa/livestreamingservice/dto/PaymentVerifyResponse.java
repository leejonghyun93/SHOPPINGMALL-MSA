package org.kosa.livestreamingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.kosa.paymentservice.dto.PaymentStatus;

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
