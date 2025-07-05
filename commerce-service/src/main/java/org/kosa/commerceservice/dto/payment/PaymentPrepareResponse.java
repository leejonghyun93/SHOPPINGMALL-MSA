package org.kosa.commerceservice.dto.payment;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentPrepareResponse {
    private String paymentId;
    private String merchantUid;
    private Integer amount;
    private String buyerName;
    private String buyerEmail;
    private String buyerTel;
    private String name;
}