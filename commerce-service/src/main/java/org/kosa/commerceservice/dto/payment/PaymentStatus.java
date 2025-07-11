package org.kosa.commerceservice.dto.payment;

public enum PaymentStatus {
    PENDING("대기중"),
    COMPLETED("완료"),
    FAILED("실패"),
    CANCELLED("취소"),
    REFUNDED("환불");

    private final String description;

    PaymentStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}