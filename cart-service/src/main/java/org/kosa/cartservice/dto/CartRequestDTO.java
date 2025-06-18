package org.kosa.cartservice.dto;


import lombok.Data;

@Data
public class CartRequestDTO {
    private String productId;
    private String productOptionId;
    private Integer quantity;
    private String userId;

    public String getProductOptionId() {
        return productOptionId != null ? productOptionId : "defaultOptionId";
    }
}
