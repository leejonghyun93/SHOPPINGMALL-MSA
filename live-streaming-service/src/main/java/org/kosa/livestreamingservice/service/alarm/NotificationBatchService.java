package org.kosa.livestreamingservice.service.alarm;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kosa.livestreamingservice.client.alarm.BroadcastServiceClient;
import org.kosa.livestreamingservice.entity.alarm.LiveBroadcastNotification;
import org.kosa.livestreamingservice.repository.alarm.LiveBroadcastNotificationRepository;
import org.kosa.livestreamingservice.repository.alarm.BroadcastRepository;
import org.kosa.livestreamingservice.entity.BroadcastEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 🔥 방송 시작 알림 배치 서비스 (기존 이메일 시스템 유지 + 헤더 알림 개선)
 * 기존 이메일 발송은 그대로 유지하면서 헤더 알림만 개선
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationBatchService {

    private final LiveBroadcastNotificationRepository notificationRepository;
    private final BroadcastRepository broadcastRepository;
    private final KafkaNotificationProducer kafkaProducer;
    private final BroadcastServiceClient broadcastServiceClient;

    /**
     * 🚀 기존 방송 시작 알림 배치 (30초마다 실행) - 기존 로직 유지
     * 이메일 발송은 기존 방식 그대로 유지
     */
    @Scheduled(fixedRate = 30000) // 30초마다 실행 (기존과 동일)
    @Transactional
    public void processBroadcastStartNotifications() {
        LocalDateTime now = LocalDateTime.now();
        log.info("=== 방송 시작 알림 배치 작업 시작: {} ===",
                now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        try {
            // 🔥 기존 방식: 현재 시간 기준으로 시작해야 할 방송들을 직접 DB에서 조회
            List<Long> startingBroadcastIds = getStartingBroadcastIds(now);

            if (startingBroadcastIds.isEmpty()) {
                log.debug("현재 시작하는 방송이 없습니다. 현재 시간: {}",
                        now.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
                return;
            }

            log.info("🎬 시작하는 방송들: {} (현재시간: {})", startingBroadcastIds,
                    now.format(DateTimeFormatter.ofPattern("HH:mm:ss")));

            // 2. 해당 방송들의 미발송 알림들 조회 (기존 메서드 사용)
            List<LiveBroadcastNotification> pendingNotifications =
                    getPendingNotificationsForBroadcasts(startingBroadcastIds);

            if (pendingNotifications.isEmpty()) {
                log.info("발송할 알림이 없습니다. 방송 ID: {}", startingBroadcastIds);
                return;
            }

            log.info("📬 발송할 알림 개수: {} (방송들: {})", pendingNotifications.size(), startingBroadcastIds);

            // 3. 각 알림에 대해 상세 로그
            pendingNotifications.forEach(notification -> {
                log.info("📧 알림 발송 대상 - ID: {}, 사용자: {}, 방송: {}",
                        notification.getNotificationId(),
                        notification.getUserId(),
                        notification.getBroadcastId());
            });

            // 4. 카프카로 대량 알림 발송 (기존 방식)
            kafkaProducer.sendBulkNotifications(pendingNotifications);

            log.info("=== ✅ 방송 시작 알림 배치 작업 완료: {}개 발송 ===", pendingNotifications.size());

        } catch (Exception e) {
            log.error("❌ 방송 시작 알림 배치 작업 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * 🔥 기존 방식: 현재 시간 기준으로 시작해야 할 방송 ID들 조회
     * 기존 로직을 그대로 유지
     */
    private List<Long> getStartingBroadcastIds(LocalDateTime now) {
        try {
            // 🔥 현재 시간 ±1분 범위에서 시작하는 방송들 조회 (기존 방식)
            LocalDateTime startTime = now.minusMinutes(1);  // 1분 전부터
            LocalDateTime endTime = now.plusMinutes(1);     // 1분 후까지

            log.debug("방송 시작 시간 범위 조회: {} ~ {}",
                    startTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                    endTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")));

            // 기존 메서드 사용
            List<BroadcastEntity> startingBroadcasts = broadcastRepository
                    .findByScheduledStartTimeBetweenAndBroadcastStatus(
                            startTime,
                            endTime,
                            "scheduled"  // 예약된 방송만
                    );

            List<Long> broadcastIds = startingBroadcasts.stream()
                    .map(BroadcastEntity::getBroadcastId)
                    .collect(Collectors.toList());

            if (!broadcastIds.isEmpty()) {
                startingBroadcasts.forEach(broadcast -> {
                    log.info("🎯 시작 예정 방송 발견 - ID: {}, 제목: {}, 시작시간: {}",
                            broadcast.getBroadcastId(),
                            broadcast.getTitle(),
                            broadcast.getScheduledStartTime().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
                });
            }

            return broadcastIds;

        } catch (Exception e) {
            log.error("시작하는 방송 조회 실패: {}", e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * 🔥 안전한 방식으로 미발송 알림 조회
     */
    private List<LiveBroadcastNotification> getPendingNotificationsForBroadcasts(List<Long> broadcastIds) {
        try {
            // 각 방송별로 개별 조회해서 합치기 (안전한 방식)
            return broadcastIds.stream()
                    .flatMap(broadcastId -> {
                        try {
                            return notificationRepository
                                    .findByBroadcastIdAndIsSentFalseAndType(broadcastId, "BROADCAST_START")
                                    .stream();
                        } catch (Exception e) {
                            log.error("방송 {}의 알림 조회 실패: {}", broadcastId, e.getMessage());
                            return List.<LiveBroadcastNotification>of().stream();
                        }
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("미발송 알림 조회 실패: {}", e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * 🔔 방송 시작 5분 전 리마인더 알림 (1분마다 실행) - 기존 유지
     */
    @Scheduled(fixedRate = 60000) // 1분마다 실행
    @Transactional
    public void processBroadcastReminderNotifications() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime reminderTime = now.plusMinutes(5); // 5분 후 시작하는 방송

        log.debug("=== 방송 리마인더 알림 체크: {} (5분 후: {}) ===",
                now.format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                reminderTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")));

        try {
            // 5분 후 시작하는 방송들 조회 (±30초 범위)
            LocalDateTime startTime = reminderTime.minusSeconds(30);
            LocalDateTime endTime = reminderTime.plusSeconds(30);

            List<BroadcastEntity> upcomingBroadcasts = broadcastRepository
                    .findByScheduledStartTimeBetweenAndBroadcastStatus(
                            startTime,
                            endTime,
                            "scheduled"
                    );

            if (!upcomingBroadcasts.isEmpty()) {
                log.info("🔔 5분 후 시작하는 방송들: {}",
                        upcomingBroadcasts.stream()
                                .map(b -> b.getBroadcastId() + "(" + b.getTitle() + ")")
                                .collect(Collectors.toList()));

                // TODO: 리마인더 알림 발송 로직 구현
                // 현재는 로그만 출력
            }

        } catch (Exception e) {
            log.error("방송 리마인더 알림 처리 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * 🧹 오래된 알림 정리 (매일 새벽 2시) - 기존 유지
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
     * 📊 알림 통계 로그 (매시간) - 기존 유지
     */
    @Scheduled(fixedRate = 3600000) // 1시간마다
    public void logNotificationStats() {
        try {
            LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);

            long broadcastStartCount = notificationRepository.countByTypeAndCreatedAtAfter("BROADCAST_START", oneHourAgo);

            // 안전한 방식으로 미발송 알림 개수 조회
            long totalPendingNotifications = 0;
            try {
                totalPendingNotifications = notificationRepository.countByIsSentFalse();
            } catch (Exception e) {
                log.warn("미발송 알림 개수 조회 실패, 기본값 사용: {}", e.getMessage());
            }

            log.info("📊 알림 통계 (최근 1시간): 신규방송알림={}, 전체대기알림={}",
                    broadcastStartCount, totalPendingNotifications);

        } catch (Exception e) {
            log.error("알림 통계 조회 실패: {}", e.getMessage());
        }
    }

    /**
     * 🔥 수동 방송 시작 알림 트리거 (테스트용) - 기존 유지
     */
    public void triggerBroadcastStartNotification(Long broadcastId) {
        log.info("🔥 수동 방송 시작 알림 트리거: broadcastId={}", broadcastId);

        try {
            List<LiveBroadcastNotification> pendingNotifications =
                    notificationRepository.findByBroadcastIdAndIsSentFalseAndType(
                            broadcastId, "BROADCAST_START"
                    );

            if (!pendingNotifications.isEmpty()) {
                kafkaProducer.sendBulkNotifications(pendingNotifications);
                log.info("✅ 수동 알림 발송 완료: {}개", pendingNotifications.size());
            } else {
                log.warn("⚠️ 발송할 알림이 없습니다: broadcastId={}", broadcastId);
            }

        } catch (Exception e) {
            log.error("❌ 수동 알림 발송 실패: broadcastId={}, error={}", broadcastId, e.getMessage());
        }
    }
}