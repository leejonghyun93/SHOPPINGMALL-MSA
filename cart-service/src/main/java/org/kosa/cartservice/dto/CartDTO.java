package org.kosa.cartservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartDTO {
    private String cartId;
    private String userId;
    private String status;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
    private List<CartItemDTO> cartItems;
    private Integer totalItems;
    private Long totalPrice;
    private Long totalDiscountPrice;
    private Long finalPrice;
    private Long deliveryFee;
}