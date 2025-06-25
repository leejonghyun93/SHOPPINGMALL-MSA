package org.kosa.livestreamingservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_broadcast_products")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BroadcastProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "broadcast_product_id")
    private Long broadcastProductId;

    @Column(name = "broadcast_id", nullable = false)
    private Long broadcastId;

    @Column(name = "product_id", nullable = false, length = 50)
    private String productId;

    @Column(name = "display_order")
    private Integer displayOrder = 1;

    @Column(name = "is_featured")
    private Boolean isFeatured = false;

    @Column(name = "special_price")
    private Integer specialPrice;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 연관관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "broadcast_id", insertable = false, updatable = false)
    private LiveBroadcast broadcast;
}