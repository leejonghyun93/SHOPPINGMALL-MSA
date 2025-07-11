package org.kosa.commerceservice.repository.payment;


import org.kosa.commerceservice.dto.payment.PaymentStatus;
import org.kosa.commerceservice.entity.payment.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, String> {

    // 결제 ID로 조회
    Optional<Payment> findByPaymentId(String paymentId);

    // 주문 ID로 조회
    Optional<Payment> findByOrderId(String orderId);

    // 아임포트 결제 고유번호로 조회
    Optional<Payment> findByInvoicePoId(String invoicePoId);

    // 결제 상태별 조회
    List<Payment> findByPaymentStatus(PaymentStatus status);

    // 주문 ID와 결제 상태로 조회
    @Query("SELECT p FROM Payment p WHERE p.orderId = :orderId AND p.paymentStatus = :status")
    Optional<Payment> findByOrderIdAndStatus(@Param("orderId") String orderId,
                                             @Param("status") PaymentStatus status);

    // 생성일 범위별 결제 조회
    @Query("SELECT p FROM Payment p WHERE p.createdDate BETWEEN :startDate AND :endDate")
    List<Payment> findByCreatedDateBetween(@Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate);
}