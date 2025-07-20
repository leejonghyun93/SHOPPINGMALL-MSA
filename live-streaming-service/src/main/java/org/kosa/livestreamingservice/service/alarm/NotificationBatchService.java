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
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 🔥 방송 시작 알림 배치 서비스 (방송 정보 포함 개선)
 * 실제 방송 정보를 함께 전달하여 이메일 null 문제 해결
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
     * 🚀 방송 시작 알림 배치 (30초마다 실행) - 방송 정보 포함하여 발송
     */
    @Scheduled(fixedRate = 30000) // 30초마다 실행
    @Transactional
    public void processBroadcastStartNotifications() {
        LocalDateTime now = LocalDateTime.now();
        log.info("=== 방송 시작 알림 배치 작업 시작: {} ===",
                now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        try {
            // 1. 현재 시간 기준으로 시작해야 할 방송들 조회
            List<BroadcastEntity> startingBroadcasts = getStartingBroadcasts(now);

            if (startingBroadcasts.isEmpty()) {
                log.debug("현재 시작하는 방송이 없습니다. 현재 시간: {}",
                        now.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
                return;
            }

            log.info("🎬 시작하는 방송들: {} (현재시간: {})",
                    startingBroadcasts.stream().map(BroadcastEntity::getBroadcastId).collect(Collectors.toList()),
                    now.format(DateTimeFormatter.ofPattern("HH:mm:ss")));

            // 2. 각 방송별로 알림 발송 처리 (방송 정보 포함)
            int totalSentNotifications = 0;

            for (BroadcastEntity broadcast : startingBroadcasts) {
                totalSentNotifications += processBroadcastNotifications(broadcast);
            }

            if (totalSentNotifications > 0) {
                log.info("=== ✅ 방송 시작 알림 배치 작업 완료: {}개 발송 ===", totalSentNotifications);
            } else {
                log.info("발송할 알림이 없습니다.");
            }

        } catch (Exception e) {
            log.error("❌ 방송 시작 알림 배치 작업 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * 🎯 개별 방송의 알림 처리 (방송 정보 포함)
     */
    private int processBroadcastNotifications(BroadcastEntity broadcast) {
        try {
            // 1. 해당 방송의 미발송 알림들 조회
            List<LiveBroadcastNotification> pendingNotifications =
                    notificationRepository.findByBroadcastIdAndIsSentFalseAndType(
                            broadcast.getBroadcastId(), "BROADCAST_START"
                    );

            if (pendingNotifications.isEmpty()) {
                log.debug("방송 {}에 대한 미발송 알림이 없습니다.", broadcast.getBroadcastId());
                return 0;
            }

            log.info("📬 발송할 알림 개수: {} (방송: {} - {})",
                    pendingNotifications.size(),
                    broadcast.getBroadcastId(),
                    broadcast.getTitle());

            // 2. 방송자 정보 조회 (기존 로직 활용)
            String broadcasterName = getBroadcasterName(broadcast);

            // 3. 각 알림에 대해 상세 로그
            pendingNotifications.forEach(notification -> {
                log.info("📧 알림 발송 대상 - ID: {}, 사용자: {}, 방송: {}",
                        notification.getNotificationId(),
                        notification.getUserId(),
                        notification.getBroadcastId());
            });

            // 4. ✅ 방송 정보를 포함하여 카프카로 알림 발송
            kafkaProducer.sendBulkNotificationsWithBroadcastInfo(
                    pendingNotifications,
                    broadcast.getTitle(),                    // 실제 방송 제목
                    broadcasterName,                         // 실제 방송자명
                    broadcast.getScheduledStartTime()        // 실제 시작시간
            );

            log.info("✅ 방송 {}의 알림 {}개 발송 완료", broadcast.getBroadcastId(), pendingNotifications.size());

            return pendingNotifications.size();

        } catch (Exception e) {
            log.error("방송 {}의 알림 처리 실패: {}", broadcast.getBroadcastId(), e.getMessage(), e);
            return 0;
        }
    }

    /**
     * 🔍 현재 시간 기준으로 시작해야 할 방송들 조회
     */
    private List<BroadcastEntity> getStartingBroadcasts(LocalDateTime now) {
        try {
            // 현재 시간 ±1분 범위에서 시작하는 방송들 조회
            LocalDateTime startTime = now.minusMinutes(1);  // 1분 전부터
            LocalDateTime endTime = now.plusMinutes(1);     // 1분 후까지

            log.debug("방송 시작 시간 범위 조회: {} ~ {}",
                    startTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                    endTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")));

            List<BroadcastEntity> startingBroadcasts = broadcastRepository
                    .findByScheduledStartTimeBetweenAndBroadcastStatus(
                            startTime,
                            endTime,
                            "scheduled"  // 예약된 방송만
                    );

            // 발견된 방송들 로그
            startingBroadcasts.forEach(broadcast -> {
                log.info("🎯 시작 예정 방송 발견 - ID: {}, 제목: {}, 시작시간: {}",
                        broadcast.getBroadcastId(),
                        broadcast.getTitle(),
                        broadcast.getScheduledStartTime().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            });

            return startingBroadcasts;

        } catch (Exception e) {
            log.error("시작하는 방송 조회 실패: {}", e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * 🎭 방송자 이름 조회 (타입 오류 수정)
     */
    private String getBroadcasterName(BroadcastEntity broadcast) {
        try {
            // broadcasterID가 String이므로 직접 사용하거나 변환 필요
            String broadcasterId = broadcast.getBroadcasterId();

            // 1. String을 Long으로 변환하여 호출 (broadcastServiceClient가 Long을 요구하는 경우)
            try {
                Long broadcasterIdLong = Long.parseLong(broadcasterId);
                String broadcasterName = broadcastServiceClient.getBroadcasterName(broadcasterIdLong);

                if (broadcasterName != null && !broadcasterName.trim().isEmpty()) {
                    return broadcasterName;
                }
            } catch (NumberFormatException e) {
                log.warn("broadcaster_id를 Long으로 변환할 수 없습니다: {}", broadcasterId);
            }

            // 2. 또는 기존 로그에서 확인된 방송자명 패턴 사용
            // 로그에서 "방송자asdasdss"가 보였으므로 이 패턴 활용
            return "방송자" + broadcasterId;

        } catch (Exception e) {
            log.warn("방송자 이름 조회 실패, 기본값 사용: broadcasterId={}, error={}",
                    broadcast.getBroadcasterId(), e.getMessage());
            return "방송자" + broadcast.getBroadcasterId();
        }
    }

    /**
     * 🔔 방송 시작 5분 전 리마인더 알림 (1분마다 실행)
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
     * 🔥 수동 방송 시작 알림 트리거 (테스트용) - 방송 정보 포함
     */
    public void triggerBroadcastStartNotification(Long broadcastId) {
        log.info("🔥 수동 방송 시작 알림 트리거: broadcastId={}", broadcastId);

        try {
            // 1. 방송 정보 조회
            BroadcastEntity broadcast = broadcastRepository.findById(broadcastId)
                    .orElseThrow(() -> new RuntimeException("방송을 찾을 수 없습니다: " + broadcastId));

            // 2. 미발송 알림 조회
            List<LiveBroadcastNotification> pendingNotifications =
                    notificationRepository.findByBroadcastIdAndIsSentFalseAndType(
                            broadcastId, "BROADCAST_START"
                    );

            if (!pendingNotifications.isEmpty()) {
                // 3. 방송 정보 포함하여 발송
                String broadcasterName = getBroadcasterName(broadcast);

                kafkaProducer.sendBulkNotificationsWithBroadcastInfo(
                        pendingNotifications,
                        broadcast.getTitle(),
                        broadcasterName,
                        broadcast.getScheduledStartTime()
                );

                log.info("✅ 수동 알림 발송 완료: {}개 (방송: {})", pendingNotifications.size(), broadcast.getTitle());
            } else {
                log.warn("⚠️ 발송할 알림이 없습니다: broadcastId={}", broadcastId);
            }

        } catch (Exception e) {
            log.error("❌ 수동 알림 발송 실패: broadcastId={}, error={}", broadcastId, e.getMessage());
        }
    }
}