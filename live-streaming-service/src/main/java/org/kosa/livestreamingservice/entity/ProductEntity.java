package org.kosa.livestreamingservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_product")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PRODUCT_ID")
    private Integer productId;

    @Column(name = "NAME", nullable = false, length = 200)
    private String name;

    @Column(name = "PRICE", nullable = false)
    private Integer price;

    @Column(name = "SALE_PRICE", nullable = false)
    private Integer salePrice;

    @Column(name = "PRODUCT_DESCRIPTION", columnDefinition = "MEDIUMTEXT")
    private String productDescription;

    @Column(name = "PRODUCT_SHORT_DESCRIPTION", length = 500)
    private String productShortDescription;

    @Column(name = "PRODUCT_STATUS", nullable = false, length = 20)
    @Builder.Default
    private String productStatus = "ACTIVE";

    @Column(name = "PRODUCT_RATING", precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal productRating = BigDecimal.valueOf(0.00);

    @Column(name = "PRODUCT_REVIEW_COUNT")
    @Builder.Default
    private Integer productReviewCount = 0;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "UPDATED_DATE")
    private LocalDateTime updatedDate;

    @Column(name = "MAIN_IMAGE", length = 500)
    private String mainImage;

    @Column(name = "VIEW_COUNT")
    @Builder.Default
    private Integer viewCount = 0;

    @Column(name = "STOCK")
    @Builder.Default
    private Integer stock = 0;

    @Column(name = "HOST_ID", nullable = false)
    private Long hostId;

    @Column(name = "category_id", nullable = false)
    private Integer categoryId;

    @Column(name = "display_yn", columnDefinition = "CHAR(1)", nullable = false)
    @Builder.Default
    private String displayYn = "Y";

    @PrePersist
    protected void onCreate() {
        createdDate = LocalDateTime.now();
        updatedDate = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedDate = LocalDateTime.now();
    }
}