// UserWithdrawalEventListener.java
package org.kosa.orderservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kosa.orderservice.dto.UserWithdrawalEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserWithdrawalEventListener {

    private final OrderService orderService;
    private final OrderWithdrawalService orderWithdrawalService;

    /**
     * 🔥 사용자 탈퇴 이벤트 처리 - 현실적인 주문 데이터 처리
     */
    @KafkaListener(
            topics = "${kafka.topic.user-withdrawal:user-withdrawal-topic}",
            containerFactory = "userWithdrawalKafkaListenerContainerFactory",
            groupId = "order-service-group"
    )
    @Transactional
    public void handleUserWithdrawal(
            @Payload UserWithdrawalEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) Integer partition,
            @Header(KafkaHeaders.OFFSET) Long offset,
            Acknowledgment acknowledgment) {

        try {
            log.info("🔔 사용자 탈퇴 이벤트 수신 - Order Service");
            log.info("📍 Kafka 정보: topic={}, partition={}, offset={}", topic, partition, offset);
            log.info("📋 이벤트 내용: eventId={}, userId={}, withdrawnId={}, withdrawalReason={}",
                    event.getEventId(), event.getUserId(), event.getWithdrawnId(), event.getWithdrawalReason());
            log.info("📅 탈퇴 일자: withdrawalDate={}, secessionDate={}",
                    event.getWithdrawalDate(), event.getSecessionDate());

            // 📝 현실적인 주문 데이터 처리 로직 실행
            orderWithdrawalService.processUserWithdrawalOrders(event);

            // ✅ 처리 완료 후 수동 커밋
            acknowledgment.acknowledge();
            log.info("✅ 사용자 탈퇴 이벤트 처리 완료: userId={}", event.getUserId());

        } catch (Exception e) {
            log.error("❌ 사용자 탈퇴 이벤트 처리 실패: userId={}, error={}",
                    event.getUserId(), e.getMessage(), e);

            // 🔥 중요한 에러인 경우 재시도, 아닌 경우 스킵
            if (isRetryableError(e)) {
                log.warn("⚠️ 재시도 가능한 에러 - 메시지를 재처리합니다: {}", e.getMessage());
                throw e; // 재시도 로직 동작
            } else {
                log.error("💀 재시도 불가능한 에러 - 메시지를 스킵합니다: {}", e.getMessage());
                acknowledgment.acknowledge(); // 스킵
            }
        }
    }

    /**
     * 🔥 재시도 가능한 에러인지 판단
     */
    private boolean isRetryableError(Exception e) {
        // 데이터베이스 연결 에러, 일시적 네트워크 에러 등은 재시도
        return e.getMessage().contains("Connection") ||
                e.getMessage().contains("Timeout") ||
                e.getMessage().contains("Deadlock") ||
                e instanceof org.springframework.dao.DataAccessException;
    }
}