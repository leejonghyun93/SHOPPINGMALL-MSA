package org.kosa.commerceservice.dto.order;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDTO {
    private String orderItemId;
    private String orderId;
    private Integer productId;
    @JsonProperty("productName")
    private String name;
    private Integer quantity;
    private String status;
    private Integer totalPrice;
    private Integer deliveryFee;
    private String imageUrl;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
}