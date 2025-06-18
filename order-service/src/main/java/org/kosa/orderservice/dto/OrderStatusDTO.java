package org.kosa.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusDTO {
    private String orderId;
    private String currentStatus;
    private String statusMessage;
    private String trackingNumber;
    private String deliveryCompany;
}