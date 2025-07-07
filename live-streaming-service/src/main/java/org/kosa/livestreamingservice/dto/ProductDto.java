package org.kosa.livestreamingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class ProductDto {

    /**
     * 방송용 상품 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BroadcastProduct {
        private Integer productId;
        private String name;
        private Integer price;
        private Integer salePrice;
        private String productDescription;
        private String mainImage;
        private Integer categoryId;
        private String categoryName;
        private Integer displayOrder;
        private Integer specialPrice;
        private Boolean isFeatured;
        private Integer stock;
        private String productRating;
        private Integer viewCount;

        // 계산된 필드들
        public Integer getDiscountAmount() {
            if (price != null && salePrice != null) {
                return price - salePrice;
            }
            return 0;
        }

        public Integer getDiscountPercent() {
            if (price != null && salePrice != null && price > 0) {
                return Math.round((float)(price - salePrice) / price * 100);
            }
            return 0;
        }

        public Integer getFinalPrice() {
            return specialPrice != null ? specialPrice : salePrice;
        }
    }
}