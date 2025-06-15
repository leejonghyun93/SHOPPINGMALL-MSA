package org.kosa.productservice.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDto {
    private String productId;
    private String categoryId;
    private String name;
    private Integer price;
    private Integer salePrice;
    private String productDescription;
    private String productShortDescription;
    private String productStatus;
    private Integer productSalesCount;
    private BigDecimal productRating;
    private Integer productReviewCount;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String mainImage;
    private Integer viewCount;
    private List<ProductImageDto> productImages;

    // 프론트엔드용 추가 필드
    private String title; // name과 동일하지만 프론트엔드 호환성
    private Integer discount; // 할인율 계산
    private Integer originalPrice; // 원가 (할인 전 가격)
    private Boolean isLive; // 라이브 상품 여부
    private String viewers; // 시청자 수
    private String image; // 메인 이미지 (mainImage와 동일)
}