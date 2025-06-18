package org.kosa.cartservice.dto;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_cart_item")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItem {

    @Id
    @Column(name = "CART_ITEM_ID", length = 50)
    private String cartItemId;

    @Column(name = "CART_ID", length = 50, nullable = false, insertable = false, updatable = false)
    private String cartId;

    @Column(name = "PRODUCT_ID", length = 50, nullable = false)
    private String productId;

    @Column(name = "PRODUCT_OPTION_ID", length = 50)
    private String productOptionId;

    @Column(name = "QUANTITY", nullable = false)
    private Integer quantity;

    @UpdateTimestamp
    @Column(name = "UPDATED_DATE")
    private LocalDateTime updatedDate;


    // 수정된 부분: insertable, updatable 제거
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CART_ID")
    private Cart cart;

    @PrePersist
    public void prePersist() {
        if (this.cartItemId == null) {
            this.cartItemId = "CI-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);
        }
        // Cart가 설정되어 있으면 cartId도 동기화
        if (this.cart != null && this.cartId == null) {
            this.cartId = this.cart.getCartId();
        }
    }
}