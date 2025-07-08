package org.kosa.commerceservice.entity.product;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_wish")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Wish {
    @Id
    @Column(name = "WISH_ID", length = 50)
    private String wishId;

    @Column(name = "USER_ID", nullable = false, length = 50)
    private String userId;

    @Column(name = "PRODUCT_ID", nullable = false, length = 50)
    private String productId;

    @Column(name = "CREATED_DATE")
    @Builder.Default
    private LocalDateTime createdDate = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (createdDate == null) {
            createdDate = LocalDateTime.now();
        }
        if (wishId == null) {
            wishId = generateWishId();
        }
    }

    private String generateWishId() {
        return "WISH_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
    }
}