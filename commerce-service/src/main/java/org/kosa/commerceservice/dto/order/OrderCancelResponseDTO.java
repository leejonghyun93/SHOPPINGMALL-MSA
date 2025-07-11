package org.kosa.commerceservice.dto.order;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCancelResponseDTO {

    private String orderId;
    private String userId;
    private String cancelReason;
    private Integer refundAmount;
    private String refundStatus; // PENDING, COMPLETED, FAILED
    private LocalDateTime cancelDate;
    private String paymentCancelId; // PG사 취소 ID
    private String message; // 응답 메시지
}