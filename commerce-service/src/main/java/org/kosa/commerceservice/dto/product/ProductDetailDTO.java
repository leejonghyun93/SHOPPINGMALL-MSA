package org.kosa.commerceservice.dto.product;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class ProductDetailDTO {
    private Integer productId;
    private String name;
    private Integer price;
    private Integer salePrice;
    private String mainImage;
    private String description;
    private Integer stock;
    private Long hostId;
    private String displayYn;
    private Integer categoryId;
}
