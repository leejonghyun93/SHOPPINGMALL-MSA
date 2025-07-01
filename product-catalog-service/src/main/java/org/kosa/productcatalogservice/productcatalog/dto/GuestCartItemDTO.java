package org.kosa.productcatalogservice.productcatalog.dto;

import lombok.Data;

@Data
public class GuestCartItemDTO {
    private Integer productId;
    private Integer quantity;
    private String productOptionId;
}
