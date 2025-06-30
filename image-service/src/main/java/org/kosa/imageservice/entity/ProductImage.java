package org.kosa.imageservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_product_image")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // AUTO_INCREMENT 대응
    @Column(name = "IMAGE_ID")
    private Integer imageId;  // int → Integer (AUTO_INCREMENT이므로 nullable)

    @Column(name = "PRODUCT_ID", nullable = false)  // FK이므로 nullable = false 제거
    private Integer productId;  // String → Integer (테이블 스키마에 맞춤)

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
    private String storageType = "LOCAL";  // 기본값 설정

    @Column(name = "IMAGE_SEQ")
    @Builder.Default
    private Integer imageSeq = 1;  // 기본값 1 설정

    @Column(name = "IS_MAIN_IMAGE", columnDefinition = "CHAR(1)")
    @Builder.Default
    private String isMainImage = "N";  // 기본값 N 설정

    @Column(name = "IMAGE_ALT", length = 200)
    private String imageAlt;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "UPDATED_DATE")
    private LocalDateTime updatedDate;

    @PrePersist
    public void onCreate() {
        this.createdDate = LocalDateTime.now();
        this.updatedDate = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedDate = LocalDateTime.now();
    }

    // 편의 메서드들
    public boolean isMainImage() {
        return "Y".equals(this.isMainImage);
    }

    public void setAsMainImage() {
        this.isMainImage = "Y";
    }

    public void setAsSubImage() {
        this.isMainImage = "N";
    }

    // StorageType enum 사용을 위한 편의 메서드
    public enum StorageType {
        LOCAL, S3, URL
    }

    public StorageType getStorageTypeEnum() {
        try {
            return StorageType.valueOf(this.storageType);
        } catch (Exception e) {
            return StorageType.LOCAL;
        }
    }

    public void setStorageTypeEnum(StorageType storageType) {
        this.storageType = storageType.name();
    }
}