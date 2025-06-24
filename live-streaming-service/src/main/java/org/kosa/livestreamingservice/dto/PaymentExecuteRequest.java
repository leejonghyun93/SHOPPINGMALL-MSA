package org.kosa.livestreamingservice.dto;

import lombok.*;
// Payment 실행 요청 DTO
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentExecuteRequest {
    private String paymentId;
    private String impUid;           // 아임포트 거래 고유번호
    private String merchantUid;      // 가맹점 주문번호
    private String orderId;
}