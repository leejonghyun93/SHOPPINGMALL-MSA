package org.kosa.commerceservice.dto.product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class WishDTO {
    private String wishId;
    private String userId;
    private String productId;
    private LocalDateTime createdDate;

    // 상품 정보 추가 (조인용)
    private String productName;
    private Integer productPrice;
    private Integer salePrice;
    private String mainImage;
    private String productStatus;
}