package org.kosa.livestreamingservice.dto.chat;


import lombok.Data;

@Data
public class ChatMessageDTO {

    private String from;           // 보낸 사람 닉네임 (닉네임)
    private String text;           // 메시지 내용 or 스티커 키워드
    private String type;           // 메시지 타입 (text, sticker 등)
    private String broadcastId;    // 방송 ID (프론트에서 임시로 넘겨도 됨)

    // 🔽 DB 저장용 추가 필드
    private String userId;         // 사용자 ID (토큰에서 파싱한 값)
    private Boolean isDeleted = false;   // 삭제 여부
    private Boolean isBlurred = false;   // 블러 처리 여부
    private Boolean isPinned = false;    // 고정 여부
    private Long productId;        // 상품 링크일 경우 상품 ID
}