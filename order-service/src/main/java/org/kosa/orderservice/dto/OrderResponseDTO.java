package org.kosa.orderservice.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponseDTO {
    private String orderId;
    private String orderStatus;
    private Integer totalAmount;
    private String message;
}