package org.kosa.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderSummaryDTO {
    private String orderId;
    private LocalDateTime orderDate;
    private String orderStatus;
    private Integer totalPrice;
    private Integer itemCount;
    private String firstItemName;
    private String statusMessage;
}