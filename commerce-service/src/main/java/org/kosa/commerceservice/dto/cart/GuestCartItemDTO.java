package org.kosa.commerceservice.dto.cart;

import lombok.Data;

@Data
public class GuestCartItemDTO {
    private Integer productId;
    private Integer quantity;
    private String productOptionId;
}
