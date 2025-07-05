package org.kosa.commerceservice.repository.order;


import org.kosa.commerceservice.entity.order.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {

    // 기본 조회 메서드들
    List<Order> findByUserIdOrderByOrderDateDesc(String userId);
    Page<Order> findByUserIdOrderByOrderDateDesc(String userId, Pageable pageable);
    List<Order> findByOrderStatusOrderByOrderDateDesc(String orderStatus);
    List<Order> findByUserIdAndOrderStatusOrderByOrderDateDesc(String userId, String orderStatus);

    // 사용자별 모든 주문 조회
    List<Order> findByUserId(String userId);

    // 사용자별 특정 상태 주문 조회
    List<Order> findByUserIdAndOrderStatus(String userId, String orderStatus);

    // 활성 주문 조회 (취소 가능한 상태들)
    @Query("SELECT o FROM Order o WHERE o.userId = :userId AND o.orderStatus IN ('PENDING', 'ORDERED', 'PAYMENT_COMPLETED', 'PREPARING', 'SHIPPED')")
    List<Order> findActiveOrdersByUserId(@Param("userId") String userId);

    // 날짜 범위별 주문 조회
    @Query("SELECT o FROM Order o WHERE o.orderDate BETWEEN :startDate AND :endDate ORDER BY o.orderDate DESC")
    List<Order> findByOrderDateBetween(@Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate);

    // 사용자의 최근 주문 조회
    Optional<Order> findTopByUserIdOrderByOrderDateDesc(String userId);

    // 주문 ID와 사용자 ID로 조회 (권한 확인용)
    Optional<Order> findByOrderIdAndUserId(String orderId, String userId);

    // 사용자별 특정 날짜 이후 주문 개수
    @Query("SELECT COUNT(o) FROM Order o WHERE o.userId = :userId AND o.orderDate >= :fromDate")
    Long countByUserIdAndOrderDateAfter(@Param("userId") String userId,
                                        @Param("fromDate") LocalDateTime fromDate);

    // 모든 주문 ID 목록 조회 (디버깅용)
    @Query("SELECT o.orderId FROM Order o ORDER BY o.orderDate DESC")
    List<String> findAllOrderIds();

    // 특정 날짜 이후 사용자 주문 조회
    @Query("SELECT o FROM Order o WHERE o.userId = :userId AND o.orderDate >= :fromDate ORDER BY o.orderDate DESC")
    List<Order> findByUserIdAndOrderDateAfter(@Param("userId") String userId, @Param("fromDate") LocalDateTime fromDate);
}