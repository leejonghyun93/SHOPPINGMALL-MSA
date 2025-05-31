package org.kosa.boardservice.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.kosa.boardservice.mapper.BoardMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class UserEventConsumer {

    private final BoardMapper boardMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "user-events", groupId = "board-service-group")
    public void consume(String message) {
        try {
            String[] parts = message.split("\\|", 2);
            String eventType = parts[0];
            JsonNode payload = objectMapper.readTree(parts[1]);

            String userId = payload.get("userId").asText();

            switch (eventType) {
                case "user.deleted":
                    boardMapper.hideAllByUserId(userId);
                    break;

                case "user.updated":
                    String newNickname = payload.get("nickname").asText();
                    boardMapper.updateNicknameByUserId(userId, newNickname);
                    break;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}