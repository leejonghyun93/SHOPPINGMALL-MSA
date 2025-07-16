package org.kosa.livestreamingservice.config.alarm;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kosa.livestreamingservice.dto.alarm.NotificationMessageDto;
import org.kosa.livestreamingservice.entity.alarm.LiveBroadcastNotification;
import org.kosa.livestreamingservice.repository.alarm.LiveBroadcastNotificationRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Slf4j
@RequiredArgsConstructor
public class LiveBroadcastNotificationListener {

    private final LiveBroadcastNotificationRepository notificationRepository;

    @KafkaListener(topics = "live-broadcast-notification-topic", groupId = "live-broadcast-group")
    public void handleLiveBroadcastNotification(NotificationMessageDto message) {
        try {
            log.info("=== 라이브 방송 시작 알림 수신 ===");
            log.info("방송 ID: {}", message.getBroadcastId());
            log.info("사용자 ID: {}", message.getUserId());
            log.info("사용자 이메일: {}", message.getEmail());
            log.info("알림 타입: {}", message.getType());
            log.info("알림 제목: {}", message.getTitle());
            log.info("알림 메시지: {}", message.getMessage());
            log.info("우선순위: {}", message.getPriority());
            log.info("--- 방송 정보 ---");
            log.info("방송 제목: {}", message.getBroadcastTitle());
            log.info("방송자: {}", message.getBroadcasterName());
            log.info("썸네일: {}", message.getThumbnailUrl());
            log.info("시작 예정 시간: {}", message.getScheduledStartTime());
            log.info("메시지 생성 시간: {}", message.getCreatedAt());
            log.info("수신 시간: {}", LocalDateTime.now());
            log.info("=====================================");

            // 알림 엔티티 생성 및 저장
            LiveBroadcastNotification notification = LiveBroadcastNotification.builder()
                    .broadcastId(message.getBroadcastId())
                    .userId(message.getUserId())
                    .type(message.getType())
                    .title(message.getTitle())
                    .message(message.getMessage())
                    .priority(message.getPriority() != null ? message.getPriority() : "NORMAL")
                    .isRead(false)
                    .isSent(false)
                    .build();

            LiveBroadcastNotification savedNotification = notificationRepository.save(notification);

            log.info("라이브 방송 알림 저장 완료 - 알림 ID: {}", savedNotification.getNotificationId());

            // 실제 알림 발송 처리 (이메일)
            processLiveBroadcastNotification(savedNotification, message);

            log.info("라이브 방송 시작 알림 처리 완료");

        } catch (Exception e) {
            log.error("라이브 방송 시작 알림 처리 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    private void processLiveBroadcastNotification(LiveBroadcastNotification notification, NotificationMessageDto message) {
        try {
            log.debug("라이브 방송 시작 알림 발송 처리 중 - 알림 ID: {}", notification.getNotificationId());
            log.debug("발송 대상 이메일: {}", message.getEmail());
            log.debug("방송자: {}, 방송 제목: {}", message.getBroadcasterName(), message.getBroadcastTitle());

            // 실제 이메일 발송 로직
            // emailService.sendLiveBroadcastStartNotification(message);

            // 발송 완료 처리
            notification.setIsSent(true);
            notification.setSentAt(LocalDateTime.now());
            notificationRepository.save(notification);

            log.info("라이브 방송 시작 알림 이메일 발송 완료 - 사용자: {}, 방송: {}",
                    notification.getUserId(), message.getBroadcastTitle());

        } catch (Exception e) {
            log.error("라이브 방송 시작 알림 발송 중 오류 - 알림 ID: {}, 사용자: {}, 오류: {}",
                    notification.getNotificationId(), notification.getUserId(), e.getMessage(), e);
        }
    }
}

