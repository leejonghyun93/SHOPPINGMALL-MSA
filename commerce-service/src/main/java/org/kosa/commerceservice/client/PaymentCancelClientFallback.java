package org.kosa.commerceservice.client;

import lombok.extern.slf4j.Slf4j;

import org.kosa.commerceservice.dto.payment.PaymentCancelRequestDTO;
import org.kosa.commerceservice.dto.payment.PaymentCancelResponseDTO;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PaymentCancelClientFallback implements PaymentCancelClient {

    @Override
    public PaymentCancelResponseDTO cancelPayment(PaymentCancelRequestDTO request) {
        log.warn("Payment Service 호출 실패 - Fallback 실행: paymentId={}", request.getPaymentId());

        return PaymentCancelResponseDTO.builder()
                .success(false)
                .cancelId(null)
                .message("Payment Service 일시적 장애로 수동 처리가 필요합니다.")
                .errorCode("SERVICE_UNAVAILABLE")
                .build();
    }
}