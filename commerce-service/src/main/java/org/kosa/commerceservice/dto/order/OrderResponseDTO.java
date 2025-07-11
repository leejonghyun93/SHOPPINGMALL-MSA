package org.kosa.commerceservice.dto.order;

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