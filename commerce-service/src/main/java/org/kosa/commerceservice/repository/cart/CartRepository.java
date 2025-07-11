package org.kosa.commerceservice.repository.cart;


import org.kosa.commerceservice.entity.cart.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, String> {

    // 사용자 ID와 상태로 장바구니 조회
    Optional<Cart> findByUserIdAndStatus(String userId, String status);

    // 활성 장바구니 조회
    @Query("SELECT c FROM Cart c WHERE c.userId = :userId AND c.status = 'ACTIVE'")
    Optional<Cart> findActiveCartByUserId(@Param("userId") String userId);

    // 사용자 ID로 장바구니 조회
    Optional<Cart> findByUserId(String userId);
}
