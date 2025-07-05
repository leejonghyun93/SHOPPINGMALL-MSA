package org.kosa.commerceservice.entity.order;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_order_cancels")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCancel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private String orderId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "reason")
    private String reason;

    @Column(name = "detail")
    private String detail;

    @Column(name = "refund_amount", nullable = false)
    private Integer refundAmount;

    @Column(name = "refund_status", nullable = false)
    private String refundStatus;

    @Column(name = "payment_id")
    private String paymentId;

    @Column(name = "payment_cancel_id")
    private String paymentCancelId;

    @Column(name = "cancel_date", nullable = false)
    private LocalDateTime cancelDate;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (cancelDate == null) {
            cancelDate = LocalDateTime.now();
        }
        if (refundStatus == null) {
            refundStatus = "PENDING";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}