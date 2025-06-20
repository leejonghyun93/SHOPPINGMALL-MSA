package org.kosa.paymentservice.dto;


import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentStatusResponse {
    private String paymentId;
    private String orderId;
    private PaymentStatus status;
    private Integer amount;
    private String paymentMethod;
    private String cardName;
    private String bankName;
    private String approvalNumber;
    private Integer installmentNumber;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
}