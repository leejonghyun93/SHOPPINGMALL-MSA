package org.kosa.paymentservice.dto;

import lombok.*;

// Payment 준비 요청 DTO
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentPrepareRequest {
    private String orderId;
    private String userId;
    private Integer amount;
    private String paymentMethod;
    private String buyerName;
    private String buyerEmail;
    private String buyerTel;
    private String productName;
}