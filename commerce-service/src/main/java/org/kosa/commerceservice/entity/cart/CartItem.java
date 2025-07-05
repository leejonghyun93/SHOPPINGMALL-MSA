package org.kosa.commerceservice.entity.cart;

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

    @Column(name = "PRODUCT_ID", nullable = false)
    private Integer productId;

    @Column(name = "PRODUCT_OPTION_ID", length = 50)
    private String productOptionId;

    @Column(name = "QUANTITY", nullable = false)
    private Integer quantity;

    @UpdateTimestamp
    @Column(name = "UPDATED_DATE")
    private LocalDateTime updatedDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CART_ID")
    private Cart cart;

    @PrePersist
    public void prePersist() {
        if (this.cartItemId == null) {
            this.cartItemId = "CI-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);
        }
        if (this.cart != null && this.cartId == null) {
            this.cartId = this.cart.getCartId();
        }
    }
}
