package org.kosa.commerceservice.dto.cart;

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
    private Integer totalPrice;
    private Integer totalDiscountPrice;
    private Integer finalPrice;
    private Integer deliveryFee;
}