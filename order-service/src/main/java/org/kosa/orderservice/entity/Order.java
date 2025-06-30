package org.kosa.orderservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tb_order")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @Column(name = "ORDER_ID", length = 50)
    private String orderId;

    @Column(name = "USER_ID", length = 50, nullable = false)
    private String userId;

    @Column(name = "ORDER_DATE", nullable = false)
    private LocalDateTime orderDate;

    //  ORDER_STATUS에 기본값 추가
    @Column(name = "ORDER_STATUS", length = 20, nullable = false)
    @Builder.Default
    private String orderStatus = "PENDING";

    @Column(name = "PHONE", length = 20)
    private String phone;

    @Column(name = "EMAIL", length = 100)
    private String email;

    //  필수 배송 정보에 기본값 추가
    @Column(name = "RECIPIENT_NAME", length = 100, nullable = false)
    @Builder.Default
    private String recipientName = "";

    @Column(name = "RECIPIENT_PHONE", length = 20, nullable = false)
    @Builder.Default
    private String recipientPhone = "";

    @Column(name = "ORDER_ZIPCODE", length = 10)
    private String orderZipcode;

    @Column(name = "ORDER_ADDRESS_DETAIL", length = 500, nullable = false)
    @Builder.Default
    private String orderAddressDetail = "";

    @Column(name = "DELIVERY_MEMO", columnDefinition = "TEXT")
    private String deliveryMemo;

    @Column(name = "TOTAL_PRICE", nullable = false)
    private Integer totalPrice;

    @Column(name = "DELIVERY_FEE")
    @Builder.Default
    private Integer deliveryFee = 0;

    @Column(name = "DISCOUNT_AMOUNT")
    @Builder.Default
    private Integer discountAmount = 0;

    @Column(name = "USED_POINT")
    @Builder.Default
    private Integer usedPoint = 0;

    //  결제 방법에 기본값 추가
    @Column(name = "PAYMENT_METHOD", length = 20, nullable = false)
    @Builder.Default
    private String paymentMethod = "CARD";

    @Column(name = "SAVED_POINT")
    @Builder.Default
    private Integer savedPoint = 0;

    @Column(name = "PAYMENT_METHOD_NAME", length = 100)
    @Builder.Default
    private String paymentMethodName = "신용카드";

    @Column(name = "SHIPPING_DATE")
    private LocalDateTime shippingDate;

    @Column(name = "ESTIMATED_DATE")
    private LocalDateTime estimatedDate;

    @Column(name = "TRACKING_NUMBER", length = 100)
    private String trackingNumber;

    // 배송 회사에 기본값 추가
    @Column(name = "DELIVERY_COMPANY", length = 100)
    @Builder.Default
    private String deliveryCompany = "샛별배송";

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "UPDATED_DATE")
    private LocalDateTime updatedDate;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<OrderItem> orderItems = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdDate = now;
        this.updatedDate = now;
        this.orderDate = now;

        //  기본값들이 null인 경우 설정
        if (this.orderStatus == null || this.orderStatus.trim().isEmpty()) {
            this.orderStatus = "PENDING";
        }

        if (this.paymentMethod == null || this.paymentMethod.trim().isEmpty()) {
            this.paymentMethod = "CARD";
        }

        if (this.paymentMethodName == null || this.paymentMethodName.trim().isEmpty()) {
            this.paymentMethodName = "신용카드";
        }

        if (this.deliveryCompany == null || this.deliveryCompany.trim().isEmpty()) {
            this.deliveryCompany = "샛별배송";
        }

        //  필수 필드들 기본값 설정
        if (this.recipientName == null) {
            this.recipientName = "";
        }

        if (this.recipientPhone == null) {
            this.recipientPhone = "";
        }

        if (this.orderAddressDetail == null) {
            this.orderAddressDetail = "";
        }

        //  숫자 필드들 기본값 설정
        if (this.deliveryFee == null) {
            this.deliveryFee = 0;
        }

        if (this.discountAmount == null) {
            this.discountAmount = 0;
        }

        if (this.usedPoint == null) {
            this.usedPoint = 0;
        }

        if (this.savedPoint == null) {
            this.savedPoint = 0;
        }

        // 주문 ID 생성 (ORDER + timestamp + random)
        if (this.orderId == null) {
            this.orderId = "ORDER" + System.currentTimeMillis() + "_" +
                    (int)(Math.random() * 1000);
        }

        //  예상 배송일 설정 (내일 오전 7시)
        if (this.estimatedDate == null) {
            this.estimatedDate = now.plusDays(1).withHour(7).withMinute(0).withSecond(0).withNano(0);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedDate = LocalDateTime.now();
    }

    // 주문 아이템 추가 편의 메서드
    public void addOrderItem(OrderItem orderItem) {
        this.orderItems.add(orderItem);
        orderItem.setOrder(this);
    }
}