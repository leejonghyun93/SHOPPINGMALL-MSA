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
public class ProductQnaAnswerDto {
    private String answerId;      // ANSWER_ID
    private String qnaId;         // QNA_ID
    private String userId;        // USER_ID
    private String content;       // CONTENT
    private String answerStatus;  // ANSWER_STATUS (ACTIVE, DELETED)
    private String isSeller;      // IS_SELLER (Y/N)
    private String authorName;    // AUTHOR_NAME
    private LocalDateTime createdDate;   // CREATED_DATE
    private LocalDateTime updatedDate;   // UPDATED_DATE

    // 프론트엔드 호환성을 위한 메서드들
    public String getId() {
        return answerId;
    }

    public String getUserName() {
        return authorName;
    }

    public String getCreatedAt() {
        return createdDate != null ? createdDate.toString() : null;
    }

    public boolean isSellerAnswer() {
        return "Y".equals(isSeller);
    }
}