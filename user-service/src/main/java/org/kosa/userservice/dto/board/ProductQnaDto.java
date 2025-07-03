package org.kosa.userservice.dto.board;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductQnaDto {
    private String qnaId;
    private String productId;
    private String userId;
    private String qnaType;
    private String title;
    private String content;
    private String qnaStatus;
    private String isSecret;
    private Integer viewCount;
    private String authorName;
    private String authorIp;
    private String password;
    private LocalDateTime answerDate;
    private String answerUserId;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;

    private List<ProductQnaAnswerDto> answers;
    private String answerContent;
    private String answerAuthorName;

    public String getId() {
        return qnaId;
    }

    public String getUserName() {
        return authorName;
    }

    public String getCreatedAt() {
        return createdDate != null ? createdDate.toString() : null;
    }

    public boolean hasAnswer() {
        return "ANSWERED".equals(qnaStatus) && answerDate != null;
    }

    public boolean isSecretQna() {
        return "Y".equals(isSecret);
    }
}