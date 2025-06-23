package org.kosa.orderservice.mapper;


import org.kosa.orderservice.dto.OrderCancel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderCancelRepository extends JpaRepository<OrderCancel, Long> {

    Optional<OrderCancel> findByOrderId(String orderId);

    List<OrderCancel> findByUserId(String userId);

    @Query("SELECT oc FROM OrderCancel oc WHERE oc.orderId = :orderId AND oc.userId = :userId")
    Optional<OrderCancel> findByOrderIdAndUserId(@Param("orderId") String orderId, @Param("userId") String userId);

    @Query("SELECT oc FROM OrderCancel oc WHERE oc.refundStatus = :status")
    List<OrderCancel> findByRefundStatus(@Param("status") String status);

    @Query("SELECT oc FROM OrderCancel oc WHERE oc.userId = :userId ORDER BY oc.cancelDate DESC")
    List<OrderCancel> findByUserIdOrderByCancelDateDesc(@Param("userId") String userId);
}