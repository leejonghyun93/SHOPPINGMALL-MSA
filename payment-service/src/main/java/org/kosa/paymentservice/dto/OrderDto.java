package org.kosa.paymentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDto {
    private String orderId;
    private String userId;
    private String merchantUid;
    private BigDecimal totalAmount;
    private String status;
    private String recipientName;
    private String recipientPhone;
    private String address;
    private String detailAddress;
    private String zipCode;
    private String deliveryMemo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}