package org.kosa.userservice.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserKafkaProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public void sendUserEvent(String eventType, String payloadJson) {
        String message = eventType + "|" + payloadJson; // 간단한 구분자 포맷
        kafkaTemplate.send("user-events", message);
    }
}