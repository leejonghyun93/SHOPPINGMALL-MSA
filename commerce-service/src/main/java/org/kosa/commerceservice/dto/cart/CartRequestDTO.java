package org.kosa.commerceservice.dto.cart;

import lombok.Data;

@Data
public class CartRequestDTO {
    private Integer productId;
    private String productOptionId;
    private Integer quantity;
    private String userId;

    public String getProductOptionId() {
        return productOptionId != null ? productOptionId : "defaultOptionId";
    }
}