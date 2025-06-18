package org.kosa.orderservice.dto;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

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

    @Column(name = "ORDER_STATUS", length = 20, nullable = false)
    private String orderStatus;

    @Column(name = "PHONE", length = 20)
    private String phone;

    @Column(name = "EMAIL", length = 100)
    private String email;

    @Column(name = "RECIPIENT_NAME", length = 100)
    private String recipientName;

    @Column(name = "RECIPIENT_PHONE", length = 20)
    private String recipientPhone;

    @Column(name = "ORDER_ZIPCODE", length = 10)
    private String orderZipcode;

    @Column(name = "ORDER_ADDRESS_DETAIL", length = 500)
    private String orderAddressDetail;

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

    @Column(name = "PAYMENT_METHOD", length = 20)
    private String paymentMethod;

    @Column(name = "SAVED_POINT")
    @Builder.Default
    private Integer savedPoint = 0;

    @Column(name = "PAYMENT_METHOD_NAME", length = 100)
    private String paymentMethodName;

    @Column(name = "SHIPPING_DATE")
    private LocalDateTime shippingDate;

    @Column(name = "ESTIMATED_DATE")
    private LocalDateTime estimatedDate;

    @Column(name = "TRACKING_NUMBER", length = 100)
    private String trackingNumber;

    @Column(name = "DELIVERY_COMPANY", length = 100)
    private String deliveryCompany;

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

        // 주문 ID 생성 (ORDER + timestamp + random)
        if (this.orderId == null) {
            this.orderId = "ORDER" + System.currentTimeMillis() + "_" +
                    (int)(Math.random() * 1000);
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