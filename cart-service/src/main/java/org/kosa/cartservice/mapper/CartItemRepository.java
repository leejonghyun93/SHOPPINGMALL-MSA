package org.kosa.cartservice.mapper;


import org.kosa.cartservice.dto.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, String> {

    Optional<CartItem> findByCartIdAndProductIdAndProductOptionId(String cartId, String productId, String productOptionId);

    Optional<CartItem> findByCartIdAndProductId(String cartId, String productId);



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
            @Param("productId") String productId,
            @Param("productOptionId") String productOptionId
    );

    List<CartItem> findByCartId(String cartId);

    // CartItem 엔티티의 cartId 필드를 기준으로 업데이트 날짜 내림차순 정렬 조회
    List<CartItem> findByCartIdOrderByUpdatedDateDesc(String cartId);

    // 또는 연관관계 Cart 객체의 cartId를 기준으로 조회할 경우
    List<CartItem> findByCart_CartIdOrderByUpdatedDateDesc(String cartId);
}