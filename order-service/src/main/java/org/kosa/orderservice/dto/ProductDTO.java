package org.kosa.orderservice.dto;

import lombok.Data;
import java.util.List;

@Data
public class ProductDTO {
    private Integer productId;
    private String name;
    private String title;
    private Long price;
    private Long salePrice;
    private Integer discountRate;
    private String mainImage;
    private List<String> images;
    private String category;
    private String deliveryType;
    private String productShortDescription;
}