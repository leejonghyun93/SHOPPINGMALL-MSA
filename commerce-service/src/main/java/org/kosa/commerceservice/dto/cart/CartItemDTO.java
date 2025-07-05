package org.kosa.commerceservice.dto.cart;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemDTO {
    private String cartItemId;
    private String cartId;
    private Integer productId;
    @Builder.Default
    private String productOptionId = "defaultOptionId";
    private Integer quantity;
    private LocalDateTime updatedDate;
    private String productName;
    private String productImage;
    private Integer productPrice;
    private Integer salePrice;
    private Integer discountRate;
    private String category;
    private String deliveryType;
    private Integer itemTotalPrice;
    private Integer itemTotalSalePrice;
}