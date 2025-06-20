package org.kosa.paymentservice.dto;

import lombok.*;

import java.time.LocalDateTime;

// 주문 서비스 알림 DTO
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderPaymentNotification {
    private String orderId;
    private String paymentId;
    private String status;
    private Integer amount;
    private LocalDateTime paidAt;
    private String approvalNumber;
    private String paymentMethod;
}