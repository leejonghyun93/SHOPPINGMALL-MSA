package org.kosa.boardservice.dto;

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
    private String qnaId;         // QNA_ID
    private String productId;     // PRODUCT_ID
    private String userId;        // USER_ID
    private String qnaType;       // QNA_TYPE (배송, 상품, 교환/반품, 기타)
    private String title;         // TITLE
    private String content;       // CONTENT
    private String qnaStatus;     // QNA_STATUS (WAITING, ANSWERED, CLOSED)
    private String isSecret;      // IS_SECRET (Y/N)
    private Integer viewCount;    // VIEW_COUNT
    private String authorName;    // AUTHOR_NAME
    private String authorIp;      // AUTHOR_IP
    private String password;      // PASSWORD (비회원용)
    private LocalDateTime answerDate;    // ANSWER_DATE
    private String answerUserId;  // ANSWER_USER_ID
    private LocalDateTime createdDate;   // CREATED_DATE
    private LocalDateTime updatedDate;   // UPDATED_DATE

    // 🔥 답변 정보 (JOIN으로 가져올 때 사용)
    private List<ProductQnaAnswerDto> answers;  // 답변 목록
    private String answerContent;    // 첫 번째 답변 내용 (간단 조회용)
    private String answerAuthorName; // 답변자명

    // 프론트엔드 호환성을 위한 메서드들
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