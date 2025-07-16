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

    // 🔥 정적 변수 제거하고 동적으로 처리
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

    // 🔥 상대 경로만 저장 (baseUrl은 서비스에서 주입)
    private static String buildCompleteImageUrl(String imageUrl, String fileName) {
        // 이미 완전한 URL인 경우
        if (imageUrl != null && imageUrl.startsWith("http")) {
            return imageUrl;
        }

        // fileName만 있는 경우 상대 경로로 반환 (나중에 서비스에서 완전한 URL로 변환)
        if (fileName != null && !fileName.trim().isEmpty()) {
            return "/api/images/products/" + fileName;
        }

        // imageUrl이 있는 경우
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            if (imageUrl.startsWith("/api/")) {
                return imageUrl;
            }
            return "/api/images/products/" + imageUrl;
        }

        return null;
    }
}