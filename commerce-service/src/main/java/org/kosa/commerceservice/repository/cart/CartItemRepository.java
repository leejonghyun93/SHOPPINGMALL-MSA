package org.kosa.commerceservice.repository.cart;

import org.kosa.commerceservice.entity.cart.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, String> {

    // 장바구니 ID, 상품 ID, 옵션 ID로 조회
    Optional<CartItem> findByCartIdAndProductIdAndProductOptionId(String cartId, Integer productId, String productOptionId);

    // 장바구니 ID와 상품 ID로 조회 (옵션 없는 경우)
    Optional<CartItem> findByCartIdAndProductId(String cartId, Integer productId);

    // 장바구니별 아이템 개수
    @Query("SELECT COUNT(ci) FROM CartItem ci WHERE ci.cartId = :cartId")
    Long countByCartId(@Param("cartId") String cartId);

    // 장바구니별 총 수량
    @Query("SELECT SUM(ci.quantity) FROM CartItem ci WHERE ci.cartId = :cartId")
    Integer sumQuantityByCartId(@Param("cartId") String cartId);

    // 특정 아이템들 삭제
    void deleteByCartIdAndCartItemIdIn(String cartId, List<String> cartItemIds);

    // 장바구니 전체 아이템 삭제
    void deleteByCartId(String cartId);

    // 사용자별 장바구니 아이템 조회 (사용자 ID로)
    @Query("SELECT ci FROM CartItem ci WHERE ci.cartId = :userId")
    List<CartItem> findByUserId(@Param("userId") String userId);

    // 사용자와 아이템 ID로 조회
    @Query("SELECT ci FROM CartItem ci WHERE ci.cartId = :userId AND ci.cartItemId = :cartItemId")
    Optional<CartItem> findByUserIdAndCartItemId(@Param("userId") String userId, @Param("cartItemId") String cartItemId);

    // Null-safe 상품 옵션 조회
    @Query("SELECT ci FROM CartItem ci WHERE ci.cartId = :cartId AND ci.productId = :productId AND " +
            "(ci.productOptionId = :productOptionId OR (ci.productOptionId IS NULL AND :productOptionId IS NULL))")
    Optional<CartItem> findByCartIdAndProductIdAndProductOptionIdNullSafe(
            @Param("cartId") String cartId,
            @Param("productId") Integer productId,
            @Param("productOptionId") String productOptionId
    );

    // 장바구니 ID로 아이템 목록 조회
    List<CartItem> findByCartId(String cartId);

    // 장바구니 ID로 아이템 목록 조회 (업데이트 날짜 역순)
    List<CartItem> findByCartIdOrderByUpdatedDateDesc(String cartId);

    // Cart 엔티티 참조로 아이템 목록 조회
    List<CartItem> findByCart_CartIdOrderByUpdatedDateDesc(String cartId);

    @Query("SELECT ci FROM CartItem ci WHERE ci.cartId = :cartId AND ci.productId IN :productIds")
    List<CartItem> findByCartIdAndProductIdIn(@Param("cartId") String cartId, @Param("productIds") List<Integer> productIds);

    /**
     * 상품 ID로 장바구니 아이템 삭제
     */
    @Modifying
    @Query("DELETE FROM CartItem ci WHERE ci.cartId = :cartId AND ci.productId IN :productIds")
    void deleteByCartIdAndProductIdIn(@Param("cartId") String cartId, @Param("productIds") List<Integer> productIds);
}