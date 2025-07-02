package org.kosa.orderservice.repository;

import org.kosa.orderservice.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, String> {

    Optional<CartItem> findByCartIdAndProductIdAndProductOptionId(String cartId, Integer productId, String productOptionId);

    Optional<CartItem> findByCartIdAndProductId(String cartId, Integer productId);

    @Query("SELECT COUNT(ci) FROM CartItem ci WHERE ci.cartId = :cartId")
    Long countByCartId(@Param("cartId") String cartId);

    @Query("SELECT SUM(ci.quantity) FROM CartItem ci WHERE ci.cartId = :cartId")
    Integer sumQuantityByCartId(@Param("cartId") String cartId);

    void deleteByCartIdAndCartItemIdIn(String cartId, List<String> cartItemIds);

    void deleteByCartId(String cartId);

    @Query("SELECT ci FROM CartItem ci WHERE ci.cartId = :userId")
    List<CartItem> findByUserId(@Param("userId") String userId);

    @Query("SELECT ci FROM CartItem ci WHERE ci.cartId = :userId AND ci.cartItemId = :cartItemId")
    Optional<CartItem> findByUserIdAndCartItemId(@Param("userId") String userId, @Param("cartItemId") String cartItemId);

    @Query("SELECT ci FROM CartItem ci WHERE ci.cartId = :cartId AND ci.productId = :productId AND " +
            "(ci.productOptionId = :productOptionId OR (ci.productOptionId IS NULL AND :productOptionId IS NULL))")
    Optional<CartItem> findByCartIdAndProductIdAndProductOptionIdNullSafe(
            @Param("cartId") String cartId,
            @Param("productId") Integer productId,
            @Param("productOptionId") String productOptionId
    );

    List<CartItem> findByCartId(String cartId);

    List<CartItem> findByCartIdOrderByUpdatedDateDesc(String cartId);

    List<CartItem> findByCart_CartIdOrderByUpdatedDateDesc(String cartId);
}