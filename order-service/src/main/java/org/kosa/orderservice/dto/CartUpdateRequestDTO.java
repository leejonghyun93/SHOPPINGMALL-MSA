package org.kosa.orderservice.dto;

import lombok.Data;

@Data
public class CartUpdateRequestDTO {
    private String cartItemId;
    private Integer quantity;
    private String userId;
}
