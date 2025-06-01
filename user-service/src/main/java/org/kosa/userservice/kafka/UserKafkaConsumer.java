package org.kosa.userservice.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.kosa.userservice.dto.UserCreatedEvent;
import org.kosa.userservice.dto.UserDeletedEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class UserKafkaConsumer {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "user-topic", groupId = "user-service")
    public void handleUserEvent(String message) {
        String[] parts = message.split("\\|", 2);
        if (parts.length != 2) {
            log.warn("Invalid message format: {}", message);
            return;
        }

        String eventType = parts[0];
        String payloadJson = parts[1];

        try {
            switch (eventType) {
                case "USER_CREATED":
                    UserCreatedEvent createdEvent = objectMapper.readValue(payloadJson, UserCreatedEvent.class);
                    log.info("User Created Event: {}", createdEvent);
                    // 필요한 비즈니스 로직 처리
                    break;

                case "USER_DELETED":
                    UserDeletedEvent deletedEvent = objectMapper.readValue(payloadJson, UserDeletedEvent.class);
                    log.info("User Deleted Event: {}", deletedEvent);
                    // 필요한 비즈니스 로직 처리
                    break;

                default:
                    log.warn("Unknown event type: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Failed to parse Kafka message: {}", message, e);
        }
    }
}
