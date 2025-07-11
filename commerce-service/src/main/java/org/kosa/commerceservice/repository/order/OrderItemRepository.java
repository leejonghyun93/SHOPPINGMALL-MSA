package org.kosa.commerceservice.repository.order;


import org.kosa.commerceservice.entity.order.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, String> {

    // 주문 ID로 주문 상품 목록 조회
    List<OrderItem> findByOrderId(String orderId);

    // 주문 ID로 주문 상품 목록 조회 (생성일순)
    @Query("SELECT oi FROM OrderItem oi WHERE oi.orderId = :orderId ORDER BY oi.createdDate")
    List<OrderItem> findByOrderIdOrderByCreatedDate(@Param("orderId") String orderId);

    // 생성일 오름차순 정렬
    @Query("SELECT oi FROM OrderItem oi WHERE oi.orderId = :orderId ORDER BY oi.createdDate ASC")
    List<OrderItem> findByOrderIdOrderByCreatedDateAsc(@Param("orderId") String orderId);

    // 상품별 주문 상품 조회
    List<OrderItem> findByProductId(Integer productId);

    // 상태별 주문 상품 조회
    List<OrderItem> findByStatus(String status);
}