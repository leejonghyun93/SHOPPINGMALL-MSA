package org.kosa.orderservice.dto;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutRequestDTO {
    private String userId;
    private String phone;
    private String email;
    private String recipientName;
    private String recipientPhone;
    private String orderZipcode;
    private String orderAddressDetail;
    private String deliveryMemo;
    private String paymentMethod;
    private String paymentMethodName;
    private Integer usedPoint;
    private Integer totalAmount;
    private List<CheckoutItemDTO> items;
    private String orderStatus;
}