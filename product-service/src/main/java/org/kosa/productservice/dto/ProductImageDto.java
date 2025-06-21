package org.kosa.productservice.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

// ProductImageDto.java - 이미지 서비스에서
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductImageDto {
    private String imageId;
    private String productId;
    private String imageUrl;    // 🔥 이 필드가 어떤 형식인지 확인 필요
    private String fileName;
    private Long fileSize;
    private String storageType;
    private Integer imageSeq;
    private String isMainImage;
    private String imageAlt;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;

    /**
     * Entity to DTO 변환 - 🔥 완전한 URL 생성하도록 수정
     */
    public static ProductImageDto from(ProductImage entity) {
        ProductImageDto dto = new ProductImageDto();
        dto.setImageId(entity.getImageId());
        dto.setProductId(entity.getProductId());

        // 🔥 완전한 URL 생성
        String completeUrl = buildCompleteImageUrl(entity.getImageUrl(), entity.getFileName());
        dto.setImageUrl(completeUrl);

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

    /**
     * 🔥 완전한 이미지 URL 생성
     */
    private static String buildCompleteImageUrl(String imageUrl, String fileName) {
        // 이미 완전한 URL인 경우
        if (imageUrl != null && imageUrl.startsWith("http")) {
            return imageUrl;
        }

        // API 경로인 경우
        if (imageUrl != null && imageUrl.startsWith("/api/")) {
            return "http://localhost:8080" + imageUrl;
        }

        // fileName으로 완전한 URL 생성
        if (fileName != null && !fileName.trim().isEmpty()) {
            return "http://localhost:8080/api/images/products/" + fileName;
        }

        // imageUrl을 fileName으로 사용
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            return "http://localhost:8080/api/images/products/" + imageUrl;
        }

        return null;
    }
}