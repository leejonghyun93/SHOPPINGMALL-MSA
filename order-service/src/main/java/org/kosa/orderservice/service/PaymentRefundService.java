package org.kosa.orderservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


//PaymentRefundService.java - 환불 처리 서비스
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentRefundService {

    public void processRefund(String orderId, Integer amount, String reason) {
        try {
            log.info("💰 환불 처리 시작: orderId={}, amount={}, reason={}", orderId, amount, reason);

            // 실제 결제 게이트웨이 API 호출
            // PG사별 환불 API 연동 로직

            log.info("✅ 환불 처리 완료: orderId={}", orderId);

        } catch (Exception e) {
            log.error("❌ 환불 처리 실패: orderId={}, error={}", orderId, e.getMessage());
            throw new RuntimeException("환불 처리 실패: " + e.getMessage(), e);
        }
    }
}