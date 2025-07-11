package org.kosa.commerceservice.dto.order;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCancelRequestDTO {

    @NotBlank(message = "주문번호는 필수입니다")
    private String orderId;

    @NotBlank(message = "사용자 ID는 필수입니다")
    private String userId;

    private String reason; // 취소 사유
    private String detail; // 상세 사유

    @NotNull(message = "환불 금액은 필수입니다")
    private Integer refundAmount;

    private String paymentId;  // 결제 ID (PG사 결제 취소용)
}