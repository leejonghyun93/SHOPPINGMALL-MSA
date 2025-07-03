package org.kosa.userservice.dto.board;

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
    private String reviewId;
    private String productId;
    private String orderId;
    private String userId;
    private String title;
    private String content;
    private Integer rating;
    private String reviewStatus;
    private Integer helpfulCount;
    private Integer viewCount;
    private String isPhoto;
    private String isVerified;
    private String authorName;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;

    public String getId() {
        return reviewId;
    }

    public String getUserName() {
        return authorName;
    }

    public String getCreatedAt() {
        return createdDate != null ? createdDate.toString() : null;
    }
}