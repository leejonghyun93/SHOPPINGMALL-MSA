package org.kosa.notificationservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kosa.notificationservice.dto.NotificationResponseDto;
import org.springframework.stereotype.Service;

/**
 * 간단한 WebSocket 알림 서비스 (실제 WebSocket 없이)
 * 나중에 WebSocket 구현시 교체
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class WebSocketNotificationService {

    /**
     * 특정 사용자에게 실시간 알림 전송 (임시 구현)
     */
    public void sendNotificationToUser(Long userId, NotificationResponseDto notification) {
        try {
            // TODO: 실제 WebSocket 구현으로 교체
            log.info("WebSocket 알림 전송 (시뮬레이션): userId={}, notificationId={}",
                    userId, notification.getNotificationId());

            // 실제로는 SimpMessagingTemplate 사용
            // messagingTemplate.convertAndSend("/user/" + userId + "/notifications", notification);

        } catch (Exception e) {
            log.error("WebSocket 알림 전송 실패: userId={}, error={}", userId, e.getMessage());
        }
    }

    /**
     * 방송 구독자들에게 실시간 알림 전송 (임시 구현)
     */
    public void sendBroadcastNotification(Long broadcastId, NotificationResponseDto notification) {
        try {
            log.info("방송 WebSocket 알림 전송 (시뮬레이션): broadcastId={}, notificationId={}",
                    broadcastId, notification.getNotificationId());

        } catch (Exception e) {
            log.error("방송 WebSocket 알림 전송 실패: broadcastId={}, error={}",
                    broadcastId, e.getMessage());
        }
    }

    /**
     * 읽지 않은 알림 개수 업데이트 (임시 구현)
     */
    public void sendUnreadCountUpdate(Long userId, long unreadCount) {
        try {
            log.info("읽지 않은 알림 개수 업데이트 (시뮬레이션): userId={}, count={}",
                    userId, unreadCount);

        } catch (Exception e) {
            log.error("읽지 않은 알림 개수 업데이트 실패: userId={}, error={}",
                    userId, e.getMessage());
        }
    }
}