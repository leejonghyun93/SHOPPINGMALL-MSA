package org.kosa.livestreamingservice.config.alarm;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

// 3. Kafka 리스너 상태 확인하는 Health Check
@Component
@Slf4j
@RequiredArgsConstructor
public class KafkaHealthChecker {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void checkKafkaConnection() {
        try {
            log.info("=== Kafka 연결 상태 확인 ===");

            // 간단한 ping 메시지 전송으로 연결 확인
            CompletableFuture<SendResult<String, Object>> future =
                    kafkaTemplate.send("health-check-topic", "ping");

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Kafka 연결 정상 - 서버 응답 확인");
                } else {
                    log.error("Kafka 연결 실패: {}", ex.getMessage());
                }
            });

        } catch (Exception e) {
            log.error("Kafka 연결 확인 중 오류: {}", e.getMessage(), e);
        }
    }
}