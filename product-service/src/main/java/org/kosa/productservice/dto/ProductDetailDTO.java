package org.kosa.productservice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductDetailDTO {
    private String productId;
    private String name;
    private Integer price;
    private Integer salePrice;
    private String mainImage;
    private String description;

}