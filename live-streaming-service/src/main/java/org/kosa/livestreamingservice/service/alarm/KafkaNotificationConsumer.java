package org.kosa.livestreamingservice.service.alarm;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kosa.livestreamingservice.client.alarm.UserServiceClient;
import org.kosa.livestreamingservice.dto.alarm.NotificationMessageDto;
import org.kosa.livestreamingservice.entity.alarm.LiveBroadcastNotification;
import org.kosa.livestreamingservice.repository.alarm.LiveBroadcastNotificationRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaNotificationConsumer {

    private final EmailService emailService;
    private final LiveBroadcastNotificationRepository notificationRepository;
    private final UserServiceClient userServiceClient;

    /**
     * 카프카에서 알림 메시지 수신 및 처리
     */
    @KafkaListener(topics = "notification-events", groupId = "notification-group")
    @Transactional
    public void consumeNotification(NotificationMessageDto message) {
        log.info("알림 메시지 수신: notificationId={}, type={}, userId={}",
                message.getNotificationId(), message.getType(), message.getUserId());

        try {
            // 1. 사용자 이메일 조회
            String userEmail = getUserEmail(message.getUserId());

            if (userEmail == null) {
                log.warn("사용자 이메일을 찾을 수 없습니다: userId={}", message.getUserId());
                return;
            }

            // 2. 이메일 발송
            sendEmailByType(message, userEmail);

            // 3. 발송 완료 처리
            markNotificationAsSent(message.getNotificationId());

            log.info("알림 처리 완료: notificationId={}, email={}***",
                    message.getNotificationId(), userEmail.substring(0, Math.min(3, userEmail.length())));

        } catch (Exception e) {
            log.error("알림 처리 실패: notificationId={}, error={}",
                    message.getNotificationId(), e.getMessage(), e);
        }
    }

    /**
     * 알림 타입별 이메일 발송
     */
    private void sendEmailByType(NotificationMessageDto message, String userEmail) {
        switch (message.getType()) {
            case "BROADCAST_START":
            case "BROADCAST_REMINDER":
                emailService.sendBroadcastStartNotification(message, userEmail);
                break;

            case "BROADCAST_END":
            case "BROADCAST_CANCEL":
            case "GENERAL":
            default:
                emailService.sendGeneralNotification(message, userEmail);
                break;
        }
    }

    /**
     * 사용자 이메일 조회 (Feign Client 사용)
     */
    private String getUserEmail(String userId) {
        try {
            // UserService에서 사용자 정보 조회
            return userServiceClient.getUserEmail(userId);

        } catch (Exception e) {
            log.error("사용자 이메일 조회 실패: userId={}, error={}", userId, e.getMessage());
            return null;
        }
    }

    /**
     * 알림 발송 완료 처리
     */
    private void markNotificationAsSent(Long notificationId) {
        try {
            List<Long> notificationIds = List.of(notificationId);
            notificationRepository.markNotificationAsSent(notificationIds, LocalDateTime.now());

        } catch (Exception e) {
            log.error("알림 발송 완료 처리 실패: notificationId={}, error={}",
                    notificationId, e.getMessage(), e);
        }
    }
}