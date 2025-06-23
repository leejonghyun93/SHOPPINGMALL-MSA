package org.kosa.orderservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

// 결제 서비스 클라이언트
@FeignClient(name = "payment-service", url = "${payment-service.url}")
public interface PaymentServiceClient {

    @PostMapping("/api/payments/{paymentId}/cancel")
    PaymentCancelResponse cancelPayment(
            @PathVariable String paymentId,
            @RequestBody PaymentCancelRequest request
    );
}
