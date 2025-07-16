package org.kosa.livestreamingservice.config.alarm;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationKafkaService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendNotification(String topic, Object message) {
        try {
            log.info("=== Kafka 메시지 전송 시작 ===");
            log.info("Topic: {}", topic);
            log.info("Message: {}", message);

            // CompletableFuture 사용 (최신 Spring Kafka 버전)
            CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, message);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("메시지 전송 성공 - Topic: {}, Offset: {}",
                            topic, result.getRecordMetadata().offset());
                } else {
                    log.error("메시지 전송 실패 - Topic: {}, Error: {}",
                            topic, ex.getMessage(), ex);
                }
            });

            log.info("=== Kafka 메시지 전송 요청 완료 ===");

        } catch (Exception e) {
            log.error("Kafka 메시지 전송 중 예외 발생: {}", e.getMessage(), e);
        }
    }
}