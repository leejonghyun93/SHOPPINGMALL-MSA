// ProductReviewDto.java - String 타입으로 수정된 버전
package org.kosa.boardservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductReviewDto {
    private String reviewId;      // REVIEW_ID
    private String productId;     // PRODUCT_ID
    private String orderId;       // ORDER_ID
    private String userId;        // USER_ID
    private String title;         // TITLE
    private String content;       // CONTENT
    private Integer rating;       // RATING
    private String reviewStatus;  // REVIEW_STATUS
    private Integer helpfulCount; // HELPFUL_COUNT
    private Integer viewCount;    // VIEW_COUNT
    private String isPhoto;       // IS_PHOTO
    private String isVerified;    // IS_VERIFIED
    private String authorName;    // AUTHOR_NAME (실제 작성자명)
    private LocalDateTime createdDate;  // CREATED_DATE
    private LocalDateTime updatedDate;  // UPDATED_DATE

    // 프론트엔드에서 사용하는 필드들 추가
    public String getId() {
        return reviewId;
    }

    public String getUserName() {
        return authorName; // authorName을 userName으로 매핑
    }

    public String getCreatedAt() {
        return createdDate != null ? createdDate.toString() : null;
    }
}