package org.kosa.orderservice.client;


import org.kosa.orderservice.dto.PaymentCancelRequestDTO;
import org.kosa.orderservice.dto.PaymentCancelResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

// 결제 서비스 클라이언트
@FeignClient(
        name = "payment-service",
        url = "${payment.service.url:http://localhost:8081}",
        fallback = PaymentCancelClientFallback.class
)
public interface PaymentCancelClient {

    @PostMapping("/api/payments/cancel")
    PaymentCancelResponseDTO cancelPayment(@RequestBody PaymentCancelRequestDTO request);
}
