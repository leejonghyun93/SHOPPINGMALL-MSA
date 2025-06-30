package org.kosa.productservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDTO {
    // 기본 상품 정보
    private Integer productId;  // int(11) AUTO_INCREMENT
    private Integer categoryId;
    private String name;
    private Integer price;
    private Integer salePrice;
    private String productDescription;
    private String productShortDescription;
    private String productStatus;
    private BigDecimal productRating;
    private Integer productReviewCount;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
    private String mainImage;
    private Integer viewCount;
    private Integer stock;
    private Long hostId;
    private String displayYn;

    // 이미지 관련
    private List<ProductImageDto> productImages;

    // 프론트엔드용 추가 필드 (기존 호환성)
    private String title; // name과 동일하지만 프론트엔드 호환성
    private Integer discount; // 할인율 계산
    private Integer originalPrice; // 원가 (할인 전 가격)
    private Boolean isLive; // 라이브 상품 여부
    private String viewers; // 시청자 수
    private String image; // 메인 이미지 (mainImage와 동일)

    // 상품 상세보기용 추가 필드들
    private String subtitle; // 부제목
    private String brand; // 브랜드명
    private String origin; // 원산지
    private String deliveryInfo; // 배송/포장 정보
    private String packaging; // 판매단위
    private String weight; // 중량/용량
    private String ingredients; // 원재료명
    private String allergyInfo; // 알레르기 정보
    private List<String> images; // 추가 이미지들 (이미지 갤러리용)
    private List<String> detailImages; // 상품 상세 이미지들
    private Integer discountRate; // 할인율 (기존 discount와 동일하지만 명확한 네이밍)
    private Integer discountPrice; // 할인가 (salePrice와 동일)
    private Double averageRating; // 평균 평점 (productRating을 Double로)
    private Integer reviewCount; // 리뷰 개수 (productReviewCount와 동일)
    private Long stockQuantity; // 재고 수량 (stock을 Long으로)
    private String categoryName; // 카테고리명
    private String mainImageUrl;

    // 편의 메서드들
    public Integer getFinalPrice() {
        if (salePrice != null && salePrice > 0) {
            return salePrice;
        }
        if (discountRate != null && discountRate > 0 && price != null) {
            return (int) Math.floor(price * (100 - discountRate) / 100.0);
        }
        return price != null ? price : 0;
    }

    public boolean hasDiscount() {
        return (discountRate != null && discountRate > 0) ||
                (salePrice != null && salePrice > 0 && price != null && salePrice < price);
    }

    public String getMainImageUrl() {
        if (images != null && !images.isEmpty()) {
            return images.get(0);
        }
        return mainImage != null ? mainImage : image;
    }

    public Double getAverageRating() {
        if (averageRating != null) {
            return averageRating;
        }
        return productRating != null ? productRating.doubleValue() : 0.0;
    }

    public Integer getReviewCount() {
        return reviewCount != null ? reviewCount :
                (productReviewCount != null ? productReviewCount : 0);
    }

    public Long getStockQuantity() {
        if (stockQuantity != null) {
            return stockQuantity;
        }
        return stock != null ? stock.longValue() : 0L;
    }

    public boolean isDisplayed() {
        return "Y".equals(displayYn);
    }
}