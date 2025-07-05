package org.kosa.commerceservice.dto.productImage;

import lombok.*;
import org.kosa.commerceservice.entity.productImage.ProductImage;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ProductImageDto {
    private Integer imageId;
    private Integer productId;
    private String imageUrl;
    private String fileName;
    private Long fileSize;
    private String storageType;
    private Integer imageSeq;
    private String isMainImage;
    private String imageAlt;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;

    public static ProductImageDto from(ProductImage entity) {
        ProductImageDto dto = new ProductImageDto();
        dto.setImageId(entity.getImageId());
        dto.setProductId(entity.getProductId());
        dto.setImageUrl(buildCompleteImageUrl(entity.getImageUrl(), entity.getFileName()));
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

    private static String buildCompleteImageUrl(String imageUrl, String fileName) {
        if (imageUrl != null && imageUrl.startsWith("http")) {
            return imageUrl;
        }
        if (imageUrl != null && imageUrl.startsWith("/api/")) {
            return "http://localhost:8080" + imageUrl;
        }
        if (fileName != null && !fileName.trim().isEmpty()) {
            return "http://localhost:8080/api/images/products/" + fileName;
        }
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            return "http://localhost:8080/api/images/products/" + imageUrl;
        }
        return null;
    }
}