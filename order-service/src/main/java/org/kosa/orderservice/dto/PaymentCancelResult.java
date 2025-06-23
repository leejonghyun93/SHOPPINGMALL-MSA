package org.kosa.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCancelResult {
    private boolean success;
    private String cancelId;
    private String message;
    private String errorCode;
}
