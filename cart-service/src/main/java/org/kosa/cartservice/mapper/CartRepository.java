package org.kosa.cartservice.mapper;


import org.kosa.cartservice.entity.Cart;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, String> {

    Optional<Cart> findByUserIdAndStatus(String userId, String status);

    // 추가: findActiveCartByUserId 메서드 구현
    @Query("SELECT c FROM Cart c WHERE c.userId = :userId AND c.status = 'ACTIVE'")
    Optional<Cart> findActiveCartByUserId(@Param("userId") String userId);

    // 추가: userId로 Cart 찾기 (removePurchasedItems에서 사용)
    Optional<Cart> findByUserId(String userId);
}