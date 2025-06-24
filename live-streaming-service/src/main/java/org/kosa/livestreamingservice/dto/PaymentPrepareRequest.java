package org.kosa.livestreamingservice.dto;

import lombok.*;

// Payment 준비 요청 DTO
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentPrepareRequest {
    private String orderId;
    private String userId;
    private Integer amount;

    // PG사 선택 추가
    private String pgProvider;      // "inicis", "kcp", "toss" 등
    private String paymentMethod;   // "card", "trans", "kakaopay" 등

    private String buyerName;
    private String buyerEmail;
    private String buyerTel;
    private String productName;

    // 추가 옵션들
    private String cardQuota;       // 할부개월 (2,3,4,5,6,10,12)
    private boolean digitalGoods;   // 디지털 상품 여부
    private String customData;      // 사용자 정의 데이터
    private String noticeUrl;       // 웹훅 URL (PG사별로 다를 수 있음)
}
