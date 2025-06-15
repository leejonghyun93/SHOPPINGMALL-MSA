package org.kosa.productservice.dto;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tb_product")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @Column(name = "PRODUCT_ID")
    private String productId;

    @Column(name = "CATEGORY_ID", nullable = false)
    private String categoryId;

    @Column(name = "NAME", nullable = false)
    private String name;

    @Column(name = "PRICE", nullable = false)
    private Integer price;

    @Column(name = "SALE_PRICE")
    private Integer salePrice;

    @Column(name = "PRODUCT_DESCRIPTION")
    private String productDescription;

    @Column(name = "PRODUCT_SHORT_DESCRIPTION")
    private String productShortDescription;

    @Column(name = "PRODUCT_STATUS")
    private String productStatus;

    @Column(name = "PRODUCT_SALES_COUNT")
    private Integer productSalesCount;

    @Column(name = "PRODUCT_RATING")
    private BigDecimal productRating;

    @Column(name = "PRODUCT_REVIEW_COUNT")
    private Integer productReviewCount;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "UPDATED_DATE")
    private LocalDateTime updatedDate;

    @Column(name = "START_DATE")
    private LocalDateTime startDate;

    @Column(name = "END_DATE")
    private LocalDateTime endDate;

    @Column(name = "MAIN_IMAGE")
    private String mainImage;

    @Column(name = "VIEW_COUNT")
    private Integer viewCount;

    // 상품 이미지들
    @OneToMany(mappedBy = "productId", fetch = FetchType.LAZY)
    private List<ProductImage> productImages = new ArrayList<>();
}