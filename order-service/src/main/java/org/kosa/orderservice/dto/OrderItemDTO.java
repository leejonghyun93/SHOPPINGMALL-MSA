package org.kosa.orderservice.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDTO {
    private String orderItemId;
    private String orderId;
    private String productId;
    private String name;
    private Integer quantity;
    private String status;
    private Integer totalPrice;
    private Integer deliveryFee;
    private String imageUrl;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
}