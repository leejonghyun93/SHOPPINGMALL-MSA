package org.kosa.livestreamingservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.kosa.paymentservice.dto.PaymentStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_payment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @Column(name = "PAYMENT_ID", length = 50)
    private String paymentId;

    @Column(name = "ORDER_ID", length = 50, nullable = false)
    private String orderId;

    @Column(name = "INVOICE_PO_ID", length = 50)
    private String invoicePoId;

    @Column(name = "PAYMENT_AMOUNT", nullable = false)
    private Integer paymentAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "PAYMENT_STATUS", length = 20, nullable = false)
    private PaymentStatus paymentStatus;

    @Column(name = "PAYMENT_METHOD", length = 20, nullable = false)
    private String paymentMethod;

    @Column(name = "BANK_NAME", length = 50)
    private String bankName;

    @Column(name = "CARD_NAME", length = 50)
    private String cardName;

    @Column(name = "PAYMENT_SECOND_AMOUNT")
    private Integer paymentSecondAmount;

    @Column(name = "PAYMENT_PC_NAME", length = 100)
    private String paymentPcName;

    @Column(name = "PAYMENT_CASH_NAME", length = 100)
    private String paymentCashName;

    @Column(name = "PAYMENT_APPROVAL_NUMBER", length = 50)
    private String paymentApprovalNumber;

    @Column(name = "PAYMENT_INSTALLMENT_NUMBER")
    private Integer paymentInstallmentNumber;

    @CreationTimestamp
    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @UpdateTimestamp
    @Column(name = "UPDATED_DATE")
    private LocalDateTime updatedDate;

    // 비즈니스 메서드
    public void updatePaymentSuccess(String approvalNumber, String cardName, String bankName) {
        this.paymentStatus = PaymentStatus.COMPLETED;
        this.paymentApprovalNumber = approvalNumber;
        this.cardName = cardName;
        this.bankName = bankName;
    }

    public void updatePaymentFailed() {
        this.paymentStatus = PaymentStatus.FAILED;
    }

    public void updatePaymentCancelled() {
        this.paymentStatus = PaymentStatus.CANCELLED;
    }
}