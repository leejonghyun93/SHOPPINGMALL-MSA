package org.kosa.orderservice.dto;

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

    // 상품 정보 (Product Service에서 조회)
    private String productName;
    private String productImage;
    private Long productPrice;
    private Long salePrice;
    private Integer discountRate;
    private String category;
    private String deliveryType;
    private Long itemTotalPrice;
    private Long itemTotalSalePrice;
}