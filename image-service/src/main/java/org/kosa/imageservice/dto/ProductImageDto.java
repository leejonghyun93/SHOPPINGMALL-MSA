package org.kosa.imageservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.kosa.imageservice.entity.ProductImage;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductImageDto {
    private String imageId;
    private String productId;
    private String imageUrl;
    private String fileName;
    private Long fileSize;
    private String storageType;
    private Integer imageSeq;
    private String isMainImage;
    private String imageAlt;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;

    /**
     * Entity to DTO 변환
     */
    public static ProductImageDto from(ProductImage entity) {
        ProductImageDto dto = new ProductImageDto();
        dto.setImageId(entity.getImageId());
        dto.setProductId(entity.getProductId());
        dto.setImageUrl(entity.getImageUrl());
        dto.setFileName(entity.getFileName());
        dto.setFileSize(entity.getFileSize());
        dto.setStorageType(entity.getStorageType());
        dto.setImageSeq(entity.getImageSeq());
        dto.setIsMainImage(entity.getIsMainImage());
        dto.setImageAlt(entity.getImageAlt());
        dto.setCreatedDate(entity.getCreatedDate());
        dto.setUpdatedDate(entity.getUpdatedDate());
        return dto;
    }
}