package org.kosa.commerceservice.client;


import org.kosa.commerceservice.dto.payment.PaymentCancelRequestDTO;
import org.kosa.commerceservice.dto.payment.PaymentCancelResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "payment-service",
        url = "${payment.service.url:http://localhost:8081}",
        fallback = PaymentCancelClientFallback.class
)
public interface PaymentCancelClient {

    @PostMapping("/api/payments/cancel")
    PaymentCancelResponseDTO cancelPayment(@RequestBody PaymentCancelRequestDTO request);
}