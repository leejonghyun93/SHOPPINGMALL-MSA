package org.kosa.livestreamingservice.dto;

import lombok.*;

// Payment 취소 요청 DTO
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentCancelRequest {
    private String reason;
    private Integer cancelAmount;    // 부분 취소 시 사용
    private String cancelRequester;
}