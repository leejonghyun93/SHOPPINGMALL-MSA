package org.kosa.orderservice.mapper;


import org.kosa.orderservice.dto.Order;
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

    // 사용자별 주문 목록 조회 (최신순)
    List<Order> findByUserIdOrderByOrderDateDesc(String userId);

    // 사용자별 주문 목록 페이징 조회
    Page<Order> findByUserIdOrderByOrderDateDesc(String userId, Pageable pageable);

    // 주문 상태별 조회
    List<Order> findByOrderStatusOrderByOrderDateDesc(String orderStatus);

    // 사용자별 특정 상태 주문 조회
    List<Order> findByUserIdAndOrderStatusOrderByOrderDateDesc(String userId, String orderStatus);

    // 날짜 범위별 주문 조회
    @Query("SELECT o FROM Order o WHERE o.orderDate BETWEEN :startDate AND :endDate ORDER BY o.orderDate DESC")
    List<Order> findByOrderDateBetween(@Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate);

    // 사용자별 최근 주문 1개 조회
    Optional<Order> findTopByUserIdOrderByOrderDateDesc(String userId);

    // 주문 ID로 사용자 검증과 함께 조회
    Optional<Order> findByOrderIdAndUserId(String orderId, String userId);

    // 특정 기간 동안의 사용자 주문 수 조회
    @Query("SELECT COUNT(o) FROM Order o WHERE o.userId = :userId AND o.orderDate >= :fromDate")
    Long countByUserIdAndOrderDateAfter(@Param("userId") String userId,
                                        @Param("fromDate") LocalDateTime fromDate);
}