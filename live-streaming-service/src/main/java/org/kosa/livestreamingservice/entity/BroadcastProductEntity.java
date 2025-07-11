package org.kosa.livestreamingservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;


import java.time.LocalDateTime;

@Entity
@Table(name = "tb_broadcast_products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BroadcastProductEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "broadcast_product_id")
    private Long broadcastProductId;

    @Column(name = "broadcast_id", nullable = false)
    private Long broadcastId;

    @Column(name = "product_id", nullable = false)
    private Integer productId;

    @Column(name = "display_order")
    @Builder.Default
    private Integer displayOrder = 1;

    @Column(name = "is_featured")
    @Builder.Default
    private Boolean isFeatured = false;

    @Column(name = "special_price")
    private Integer specialPrice;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 연관관계 매핑 (필요한 경우)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "broadcast_id", insertable = false, updatable = false)
    private BroadcastEntity broadcast;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", insertable = false, updatable = false)
    private ProductEntity product;
}