package org.kosa.paymentservice.repository;

import org.kosa.paymentservice.dto.PaymentStatus;
import org.kosa.paymentservice.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, String> {

    Optional<Payment> findByPaymentId(String paymentId);

    Optional<Payment> findByOrderId(String orderId);

    Optional<Payment> findByInvoicePoId(String invoicePoId);

    List<Payment> findByPaymentStatus(PaymentStatus status);

    @Query("SELECT p FROM Payment p WHERE p.orderId = :orderId AND p.paymentStatus = :status")
    Optional<Payment> findByOrderIdAndStatus(@Param("orderId") String orderId,
                                             @Param("status") PaymentStatus status);

    @Query("SELECT p FROM Payment p WHERE p.createdDate BETWEEN :startDate AND :endDate")
    List<Payment> findByCreatedDateBetween(@Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate);
}