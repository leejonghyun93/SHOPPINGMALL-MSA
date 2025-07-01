package org.kosa.productcatalogservice.productcatalog.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ProductDTO {
    private Integer productId;
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

    // 프론트엔드용 추가 필드
    private String title;
    private Integer discount;
    private Integer originalPrice;
    private Boolean isLive;
    private String viewers;
    private String image;
    private String subtitle;
    private String brand;
    private String origin;
    private String deliveryInfo;
    private String packaging;
    private String weight;
    private String ingredients;
    private String allergyInfo;
    private List<String> images;
    private List<String> detailImages;
    private Integer discountRate;
    private Integer discountPrice;
    private Double averageRating;
    private Integer reviewCount;
    private Long stockQuantity;
    private String categoryName;
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