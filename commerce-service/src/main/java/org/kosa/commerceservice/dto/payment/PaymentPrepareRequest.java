package org.kosa.commerceservice.dto.payment;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentPrepareRequest {
    private String orderId;
    private String userId;
    private Integer amount;
    private String pgProvider;
    private String paymentMethod;
    private String buyerName;
    private String buyerEmail;
    private String buyerTel;
    private String productName;
    private String cardQuota;
    private boolean digitalGoods;
    private String customData;
    private String noticeUrl;
}
