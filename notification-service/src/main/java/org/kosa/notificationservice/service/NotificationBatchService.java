package org.kosa.notificationservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kosa.notificationservice.client.BroadcastServiceClient;
import org.kosa.notificationservice.entity.LiveBroadcastNotification;
import org.kosa.notificationservice.repository.LiveBroadcastNotificationRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 🔥 방송 시작 알림 배치 서비스 (핵심!)
 * 정시에 방송 시작 알림을 자동으로 발송
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationBatchService {

    private final LiveBroadcastNotificationRepository notificationRepository;
    private final KafkaNotificationProducer kafkaProducer;
    private final BroadcastServiceClient broadcastServiceClient;

    /**
     * 🚀 방송 시작 알림 배치 (1분마다 실행)
     * 핵심 기능: 방송이 시작될 때 구독자들에게 자동 알림 발송
     */
    @Scheduled(fixedRate = 60000) // 1분마다 실행
    @Transactional
    public void processBroadcastStartNotifications() {
        log.info("=== 방송 시작 알림 배치 작업 시작 ===");

        try {
            // 1. 지금 시작하는 방송들 조회
            List<Long> startingBroadcastIds = broadcastServiceClient.getBroadcastsStartingNow();

            if (startingBroadcastIds.isEmpty()) {
                log.debug("현재 시작하는 방송이 없습니다.");
                return;
            }

            log.info("시작하는 방송들: {}", startingBroadcastIds);

            // 2. 해당 방송들의 미발송 알림들 조회
            List<LiveBroadcastNotification> pendingNotifications =
                    notificationRepository.findByBroadcastIdInAndIsSentFalseAndType(
                            startingBroadcastIds,
                            "BROADCAST_START"
                    );

            if (pendingNotifications.isEmpty()) {
                log.info("발송할 알림이 없습니다.");
                return;
            }

            log.info("발송할 알림 개수: {}", pendingNotifications.size());

            // 3. 카프카로 대량 알림 발송
            kafkaProducer.sendBulkNotifications(pendingNotifications);

            log.info("=== 방송 시작 알림 배치 작업 완료: {}개 발송 ===", pendingNotifications.size());

        } catch (Exception e) {
            log.error("방송 시작 알림 배치 작업 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * 🔔 방송 시작 10분 전 리마인더 알림 (5분마다 실행)
     */
    @Scheduled(fixedRate = 300000) // 5분마다 실행
    @Transactional
    public void processBroadcastReminderNotifications() {
        log.info("=== 방송 리마인더 알림 배치 작업 시작 ===");

        try {
            // 10분 후 시작하는 방송들의 리마인더 알림 처리
            // TODO: 구현 필요시 추가

            log.info("=== 방송 리마인더 알림 배치 작업 완료 ===");

        } catch (Exception e) {
            log.error("방송 리마인더 알림 배치 작업 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * 🧹 오래된 알림 정리 (매일 새벽 2시)
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupOldNotifications() {
        log.info("=== 오래된 알림 정리 작업 시작 ===");

        try {
            // 30일 이전 알림들 삭제
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);

            List<LiveBroadcastNotification> oldNotifications =
                    notificationRepository.findByCreatedAtBetween(
                            LocalDateTime.of(2000, 1, 1, 0, 0),
                            cutoffDate
                    );

            if (!oldNotifications.isEmpty()) {
                notificationRepository.deleteAll(oldNotifications);
                log.info("{}개의 오래된 알림을 정리했습니다.", oldNotifications.size());
            }

            log.info("=== 오래된 알림 정리 작업 완료 ===");

        } catch (Exception e) {
            log.error("오래된 알림 정리 작업 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * 📊 알림 통계 로그 (매시간)
     */
    @Scheduled(fixedRate = 3600000) // 1시간마다
    public void logNotificationStats() {
        try {
            LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);

            long broadcastStartCount = notificationRepository.countByTypeAndCreatedAtAfter("BROADCAST_START", oneHourAgo);
            long totalUnread = notificationRepository.countByUserIdAndIsReadFalse(null); // 전체 통계

            log.info("📊 알림 통계 (최근 1시간): 방송시작={}, 전체미읽음={}",
                    broadcastStartCount, totalUnread);

        } catch (Exception e) {
            log.error("알림 통계 조회 실패: {}", e.getMessage());
        }
    }
}