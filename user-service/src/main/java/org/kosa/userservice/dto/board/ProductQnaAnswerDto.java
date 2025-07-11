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
public class ProductQnaAnswerDto {
    private String answerId;
    private String qnaId;
    private String userId;
    private String content;
    private String answerStatus;
    private String isSeller;
    private String authorName;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;

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