package org.kosa.orderservice.dto;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_order_item")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @Column(name = "ORDER_ITEM_ID", length = 50)
    private String orderItemId;

    @Column(name = "ORDER_ID", length = 50, nullable = false)
    private String orderId;

    @Column(name = "PRODUCT_ID", length = 50, nullable = false)
    private String productId;

    @Column(name = "NAME", length = 200, nullable = false)
    private String name;

    @Column(name = "QUANTITY", nullable = false)
    private Integer quantity;

    @Column(name = "STATUS", length = 20, nullable = false)
    private String status;

    @Column(name = "TOTAL_PRICE", nullable = false)
    private Integer totalPrice;

    @Column(name = "DELIVERY_FEE")
    @Builder.Default
    private Integer deliveryFee = 0;

    @Column(name = "IMAGE_URL", length = 500)
    private String imageUrl;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "UPDATED_DATE")
    private LocalDateTime updatedDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ORDER_ID", insertable = false, updatable = false)
    private Order order;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdDate = now;
        this.updatedDate = now;

        // 주문 아이템 ID 생성
        if (this.orderItemId == null) {
            this.orderItemId = "ITEM" + System.currentTimeMillis() + "_" +
                    (int)(Math.random() * 1000);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedDate = LocalDateTime.now();
    }
}
