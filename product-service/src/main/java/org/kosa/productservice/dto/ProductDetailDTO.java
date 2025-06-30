package org.kosa.productservice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductDetailDTO {
    private Integer productId;  // int(11) AUTO_INCREMENT
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