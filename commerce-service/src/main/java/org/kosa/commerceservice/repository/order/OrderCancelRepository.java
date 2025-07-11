package org.kosa.commerceservice.repository.order;

import org.kosa.commerceservice.entity.order.OrderCancel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderCancelRepository extends JpaRepository<OrderCancel, Long> {

    // 주문 ID로 취소 정보 조회
    Optional<OrderCancel> findByOrderId(String orderId);

    // 사용자 ID로 취소 목록 조회
    List<OrderCancel> findByUserId(String userId);

    // 주문 ID와 사용자 ID로 취소 정보 조회
    @Query("SELECT oc FROM OrderCancel oc WHERE oc.orderId = :orderId AND oc.userId = :userId")
    Optional<OrderCancel> findByOrderIdAndUserId(@Param("orderId") String orderId, @Param("userId") String userId);

    // 환불 상태별 취소 목록 조회
    @Query("SELECT oc FROM OrderCancel oc WHERE oc.refundStatus = :status")
    List<OrderCancel> findByRefundStatus(@Param("status") String status);

    // 사용자별 취소 목록 조회 (취소일 역순)
    @Query("SELECT oc FROM OrderCancel oc WHERE oc.userId = :userId ORDER BY oc.cancelDate DESC")
    List<OrderCancel> findByUserIdOrderByCancelDateDesc(@Param("userId") String userId);
}