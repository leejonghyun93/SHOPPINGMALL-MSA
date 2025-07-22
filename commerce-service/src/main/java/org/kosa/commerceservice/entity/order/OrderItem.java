package org.kosa.commerceservice.entity.order;

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

    @Column(name = "PRODUCT_ID", nullable = false)
    private Integer productId;

    @Column(name = "NAME", length = 200, nullable = false)
    private String name;

    @Column(name = "QUANTITY", nullable = false)
    private Integer quantity;

    @Column(name = "STATUS", length = 20, nullable = false)
    private String status = "주문완료";

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

        if (this.orderItemId == null) {
            this.orderItemId = "ITEM" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedDate = LocalDateTime.now();
    }
}
