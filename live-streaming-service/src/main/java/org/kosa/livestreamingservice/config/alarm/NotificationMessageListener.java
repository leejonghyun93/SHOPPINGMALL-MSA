package org.kosa.livestreamingservice.config.alarm;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kosa.livestreamingservice.dto.alarm.NotificationMessageDto;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationMessageListener {

    @KafkaListener(topics = "notification-topic", groupId = "notification-group")
    public void handleNotificationMessage(NotificationMessageDto message) {
        try {
            log.info("=== 알림 메시지 수신 ===");
            log.info("메시지 타입: {}", message.getType());
            log.info("사용자 ID: {}", message.getUserId());
            log.info("사용자 이메일: {}", message.getEmail());
            log.info("제목: {}", message.getTitle());
            log.info("메시지: {}", message.getMessage());
            log.info("우선순위: {}", message.getPriority());
            log.info("--- 방송 정보 ---");
            log.info("방송 ID: {}", message.getBroadcastId());
            log.info("방송 제목: {}", message.getBroadcastTitle());
            log.info("방송자: {}", message.getBroadcasterName());
            log.info("수신 시간: {}", LocalDateTime.now());
            log.info("=====================");

            // 메시지 처리 로직
            processNotification(message);

            log.info("알림 메시지 처리 완료");

        } catch (Exception e) {
            log.error("알림 메시지 처리 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    private void processNotification(NotificationMessageDto message) {
        log.debug("알림 처리 중...");
        log.debug("처리 대상: 사용자 {} -> 방송 '{}'", message.getUserId(), message.getBroadcastTitle());

        // 실제 알림 처리 로직
        // 예: 이메일 발송, 웹소켓 알림 등
    }
}