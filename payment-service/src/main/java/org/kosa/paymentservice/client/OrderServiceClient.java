package org.kosa.paymentservice.client;

import org.kosa.paymentservice.dto.OrderDto;
import org.kosa.paymentservice.dto.OrderPaymentNotification;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@FeignClient(name = "order-service", url = "${order-service.url:http://localhost:8080}")
public interface OrderServiceClient {

    /**
     * 상점 거래번호(merchantUid)로 주문 정보 조회
     *
     * @param merchantUid 상점 거래번호
     * @return 주문 정보
     */
    @GetMapping("/api/orders/merchant/{merchantUid}")
    OrderDto getOrderByMerchantUid(@PathVariable("merchantUid") String merchantUid);

    /**
     * 주문 상태를 결제 완료로 업데이트
     *
     * @param orderId 주문 ID
     * @param paymentId 결제 ID
     */
    @PutMapping("/api/orders/{orderId}/payment-completed")
    void updateOrderPaymentCompleted(
            @PathVariable("orderId") String orderId,
            @RequestParam("paymentId") String paymentId
    );

    /**
     * 주문을 취소 상태로 업데이트
     *
     * @param orderId 주문 ID
     * @param cancelReason 취소 사유
     */
    @PutMapping("/api/orders/{orderId}/cancelled")
    void updateOrderCancelled(
            @PathVariable("orderId") String orderId,
            @RequestParam("reason") String cancelReason
    );
}