package org.kosa.productservice.dto;

import lombok.Data;

@Data
public class GuestCartItemDTO {
    private String productId;
    private Integer quantity;
    private String productOptionId; // 옵션이 없으면 null 가능
}