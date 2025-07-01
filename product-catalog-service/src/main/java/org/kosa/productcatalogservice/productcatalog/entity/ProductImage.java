package org.kosa.productcatalogservice.productcatalog.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_product_image")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "IMAGE_ID")
    private Integer imageId;

    @Column(name = "PRODUCT_ID")
    private Integer productId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PRODUCT_ID", insertable = false, updatable = false)
    private Product product;

    @Column(name = "IMAGE_URL", length = 500)
    private String imageUrl;

    @Column(name = "FILE_PATH", length = 500)
    private String filePath;

    @Column(name = "FILE_NAME", length = 200)
    private String fileName;

    @Column(name = "FILE_SIZE")
    private Long fileSize;

    @Column(name = "STORAGE_TYPE", length = 20)
    @Builder.Default
    private String storageType = "LOCAL";

    @Column(name = "IMAGE_SEQ", nullable = false)
    @Builder.Default
    private Integer imageSeq = 1;

    @Column(name = "IS_MAIN_IMAGE", columnDefinition = "char(1)")
    @Builder.Default
    private String isMainImage = "N";

    @Column(name = "IMAGE_ALT", length = 200)
    private String imageAlt;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "UPDATED_DATE")
    private LocalDateTime updatedDate;

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