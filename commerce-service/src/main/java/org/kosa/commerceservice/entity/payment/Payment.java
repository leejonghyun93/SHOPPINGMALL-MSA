package org.kosa.commerceservice.entity.payment;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.kosa.commerceservice.dto.payment.PaymentStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_payment")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @Column(name = "PAYMENT_ID", length = 50)
    private String paymentId;

    @Column(name = "ORDER_ID", length = 50, nullable = false)
    private String orderId;

    @Column(name = "INVOICE_PO_ID", length = 100)
    private String invoicePoId;

    @Column(name = "PAYMENT_AMOUNT", nullable = false)
    private Integer paymentAmount;

    @Column(name = "PAYMENT_SECOND_AMOUNT")
    private Integer paymentSecondAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "PAYMENT_STATUS", nullable = false)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Column(name = "PAYMENT_METHOD", length = 50)
    private String paymentMethod;

    @Column(name = "CARD_NAME", length = 100)
    private String cardName;

    @Column(name = "BANK_NAME", length = 100)
    private String bankName;

    @Column(name = "PAYMENT_APPROVAL_NUMBER", length = 100)
    private String paymentApprovalNumber;

    @Column(name = "PAYMENT_INSTALLMENT_NUMBER")
    private Integer paymentInstallmentNumber;

    @Column(name = "CREATED_DATE", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "UPDATED_DATE")
    private LocalDateTime updatedDate;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdDate = now;
        this.updatedDate = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedDate = LocalDateTime.now();
    }

    public void updatePaymentCancelled() {
        this.paymentStatus = PaymentStatus.CANCELLED;
        this.updatedDate = LocalDateTime.now();
    }

    public void updatePaymentCompleted() {
        this.paymentStatus = PaymentStatus.COMPLETED;
        this.updatedDate = LocalDateTime.now();
    }
}