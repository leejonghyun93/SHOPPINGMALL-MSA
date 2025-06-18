package org.kosa.orderservice.mapper;


import org.kosa.orderservice.dto.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, String> {

    // 주문 ID로 주문 아이템 목록 조회
    List<OrderItem> findByOrderIdOrderByCreatedDateAsc(String orderId);

    // 상품 ID로 주문 아이템 조회
    List<OrderItem> findByProductIdOrderByCreatedDateDesc(String productId);

    // 주문 아이템 상태별 조회
    List<OrderItem> findByStatusOrderByCreatedDateDesc(String status);

    // 특정 주문의 특정 상품 조회
    List<OrderItem> findByOrderIdAndProductId(String orderId, String productId);

    // 주문별 아이템 수 조회
    @Query("SELECT COUNT(oi) FROM OrderItem oi WHERE oi.orderId = :orderId")
    Long countByOrderId(@Param("orderId") String orderId);

    // 주문별 총 수량 조회
    @Query("SELECT SUM(oi.quantity) FROM OrderItem oi WHERE oi.orderId = :orderId")
    Integer sumQuantityByOrderId(@Param("orderId") String orderId);

    // 주문별 총 금액 조회
    @Query("SELECT SUM(oi.totalPrice) FROM OrderItem oi WHERE oi.orderId = :orderId")
    Integer sumTotalPriceByOrderId(@Param("orderId") String orderId);
}