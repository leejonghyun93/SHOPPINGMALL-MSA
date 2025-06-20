package org.kosa.paymentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentVerifyRequest {

    /**
     * 아임포트 결제 고유번호
     */
    private String impUid;

    /**
     * 상점 거래번호 (주문번호)
     */
    private String merchantUid;
}