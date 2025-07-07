package org.kosa.livestreamingservice.service.alarm;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kosa.livestreamingservice.dto.alarm.NotificationMessageDto;
import org.kosa.livestreamingservice.entity.alarm.LiveBroadcastNotification;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaNotificationProducer {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    // 카프카 토픽명 (application.yml에서 설정한 것)
    private static final String NOTIFICATION_TOPIC = "notification-events";

    /**
     * 대량 알림 발송 (배치에서 사용)
     */
    public void sendBulkNotifications(List<LiveBroadcastNotification> notifications) {
        log.info("{}개의 알림을 카프카로 발송 시작", notifications.size());

        for (LiveBroadcastNotification notification : notifications) {
            try {
                NotificationMessageDto message = convertToMessage(notification);

                // 카프카로 메시지 발송
                kafkaTemplate.send(NOTIFICATION_TOPIC, message);

                log.debug("알림 발송 완료: notificationId={}, userId={}",
                        notification.getNotificationId(), notification.getUserId());

            } catch (Exception e) {
                log.error("알림 발송 실패: notificationId={}, error={}",
                        notification.getNotificationId(), e.getMessage(), e);
            }
        }

        log.info("{}개의 알림 카프카 발송 완료", notifications.size());
    }

    /**
     * 단일 알림 발송
     */
    public void sendSingleNotification(LiveBroadcastNotification notification) {
        try {
            NotificationMessageDto message = convertToMessage(notification);
            kafkaTemplate.send(NOTIFICATION_TOPIC, message);

            log.info("단일 알림 발송: notificationId={}", notification.getNotificationId());

        } catch (Exception e) {
            log.error("단일 알림 발송 실패: notificationId={}, error={}",
                    notification.getNotificationId(), e.getMessage(), e);
        }
    }

    /**
     * Entity를 Kafka 메시지 DTO로 변환
     */
    private NotificationMessageDto convertToMessage(LiveBroadcastNotification notification) {
        return NotificationMessageDto.builder()
                .notificationId(notification.getNotificationId())
                .broadcastId(notification.getBroadcastId())
                .userId(notification.getUserId())
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .priority(notification.getPriority())
                .createdAt(notification.getCreatedAt())
                // 방송 정보는 나중에 추가 (현재는 기본 정보만)
                .build();
    }
}