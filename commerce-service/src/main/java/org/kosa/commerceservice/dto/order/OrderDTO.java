package org.kosa.commerceservice.dto.order;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO {
    private String orderId;
    private String userId;
    private LocalDateTime orderDate;
    private String orderStatus;
    private String phone;
    private String email;
    private String recipientName;
    private String recipientPhone;
    private String orderZipcode;
    private String orderAddressDetail;
    private String deliveryMemo;
    private Integer totalPrice;
    private Integer deliveryFee;
    private Integer discountAmount;
    private Integer usedPoint;
    private String paymentMethod;
    private Integer savedPoint;
    private String paymentMethodName;
    private LocalDateTime shippingDate;
    private LocalDateTime estimatedDate;
    private String trackingNumber;
    private String deliveryCompany;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
    @JsonProperty("items")
    private List<OrderItemDTO> items;

    public void setOrderItems(List<OrderItemDTO> orderItems) {
        this.items = orderItems;
    }

    public List<OrderItemDTO> getOrderItems() {
        return this.items;
    }
}