package org.kosa.livestreamingservice.service.alarm;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kosa.livestreamingservice.dto.alarm.NotificationCreateDto;
import org.kosa.livestreamingservice.dto.alarm.NotificationResponseDto;
import org.kosa.livestreamingservice.entity.alarm.LiveBroadcastNotification;
import org.kosa.livestreamingservice.repository.alarm.BroadcastRepository;
import org.kosa.livestreamingservice.repository.alarm.LiveBroadcastNotificationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class NotificationService {
    private final BroadcastRepository broadcastRepository;
    private final LiveBroadcastNotificationRepository notificationRepository;
    private final KafkaNotificationProducer kafkaProducer;

    /**
     * 새 알림 생성
     */
    public NotificationResponseDto createNotification(NotificationCreateDto createDto) {
        log.info("알림 생성: broadcastId={}, userId={}, type={}",
                createDto.getBroadcastId(), createDto.getUserId(), createDto.getType());

        // 중복 알림 체크
        if (isDuplicateNotification(createDto)) {
            log.warn("중복 알림 요청: broadcastId={}, userId={}, type={}",
                    createDto.getBroadcastId(), createDto.getUserId(), createDto.getType());
            throw new IllegalArgumentException("이미 등록된 알림입니다.");
        }

        // 알림 엔티티 생성
        LiveBroadcastNotification notification = LiveBroadcastNotification.builder()
                .broadcastId(createDto.getBroadcastId())
                .userId(createDto.getUserId())
                .type(createDto.getType())
                .title(createDto.getTitle())
                .message(createDto.getMessage())
                .priority(createDto.getPriority() != null ? createDto.getPriority() : "NORMAL")
                .isSent(false)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        // DB 저장
        LiveBroadcastNotification saved = notificationRepository.save(notification);

        log.info("알림 생성 완료: notificationId={}", saved.getNotificationId());
        return convertToResponseDto(saved);
    }

    /**
     * 대량 알림 생성 (방송 시작시 구독자들에게)
     */
    public List<NotificationResponseDto> createBulkNotifications(List<NotificationCreateDto> createDtos) {
        log.info("대량 알림 생성: {}개", createDtos.size());

        List<LiveBroadcastNotification> notifications = createDtos.stream()
                .map(dto -> LiveBroadcastNotification.builder()
                        .broadcastId(dto.getBroadcastId())
                        .userId(dto.getUserId())
                        .type(dto.getType())
                        .title(dto.getTitle())
                        .message(dto.getMessage())
                        .priority(dto.getPriority() != null ? dto.getPriority() : "NORMAL")
                        .isSent(false)
                        .isRead(false)
                        .createdAt(LocalDateTime.now())
                        .build())
                .collect(Collectors.toList());

        // 대량 저장
        List<LiveBroadcastNotification> savedNotifications =
                (List<LiveBroadcastNotification>) notificationRepository.saveAll(notifications);

        log.info("대량 알림 생성 완료: {}개", savedNotifications.size());

        return savedNotifications.stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * 사용자별 알림 목록 조회 (페이징)
     */
    @Transactional(readOnly = true)
    public Page<NotificationResponseDto> getUserNotifications(String userId, Pageable pageable) {
        Page<LiveBroadcastNotification> notifications =
                notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);

        return notifications.map(this::convertToResponseDto);
    }

    /**
     * 읽지 않은 알림 목록 조회
     */
    @Transactional(readOnly = true)
    public List<NotificationResponseDto> getUnreadNotifications(String userId) {
        List<LiveBroadcastNotification> notifications =
                notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);

        return notifications.stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * 읽지 않은 알림 개수 조회
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(String userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    /**
     * 알림 구독 취소 (삭제)
     */
    public void deleteNotification(String userId, Long broadcastId, String type) {
        notificationRepository.deleteByUserIdAndBroadcastIdAndType(userId, broadcastId, type);
        log.info("알림 구독 취소: userId={}, broadcastId={}, type={}", userId, broadcastId, type);
    }

    /**
     * 즉시 알림 발송 (카프카로)
     */
    public void sendNotificationNow(Long notificationId) {
        LiveBroadcastNotification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("알림을 찾을 수 없습니다: " + notificationId));

        kafkaProducer.sendSingleNotification(notification);
        log.info("즉시 알림 발송: notificationId={}", notificationId);
    }

    /**
     * 방송별 알림 신청자 수 조회
     */
    @Transactional(readOnly = true)
    public long getBroadcastSubscriberCount(Long broadcastId) {
        return notificationRepository.countByBroadcastId(broadcastId);
    }

    /**
     * 특정 기간 알림 통계
     */
    @Transactional(readOnly = true)
    public long getNotificationStats(String type, LocalDateTime fromDate) {
        return notificationRepository.countByTypeAndCreatedAtAfter(type, fromDate);
    }

    /**
     * 중복 알림 체크
     */
    private boolean isDuplicateNotification(NotificationCreateDto createDto) {
        return notificationRepository.existsByUserIdAndBroadcastIdAndType(
                createDto.getUserId(),
                createDto.getBroadcastId(),
                createDto.getType()
        );
    }

    /**
     * 모든 알림 읽음 처리
     */
    @Transactional
    public int markAllAsRead(String userId) {
        log.info("모든 알림 읽음 처리: userId={}", userId);

        try {
            // 사용자의 읽지 않은 알림들 조회
            List<LiveBroadcastNotification> unreadNotifications =
                    notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);

            if (unreadNotifications.isEmpty()) {
                log.info("읽지 않은 알림이 없습니다: userId={}", userId);
                return 0;
            }

            // 모든 알림을 읽음 처리
            LocalDateTime readAt = LocalDateTime.now();
            unreadNotifications.forEach(notification -> {
                notification.setIsRead(true);
                notification.setReadAt(readAt);
            });

            // 일괄 저장
            notificationRepository.saveAll(unreadNotifications);

            log.info("모든 알림 읽음 처리 완료: userId={}, count={}", userId, unreadNotifications.size());
            return unreadNotifications.size();

        } catch (Exception e) {
            log.error("모든 알림 읽음 처리 실패: userId={}, error={}", userId, e.getMessage(), e);
            throw new RuntimeException("모든 알림 읽음 처리 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 특정 알림 읽음 처리 (기존 메서드 개선)
     */
    @Transactional
    public boolean markAsRead(Long notificationId, String userId) {
        try {
            int updatedRows = notificationRepository.markAsRead(notificationId, userId, LocalDateTime.now());

            if (updatedRows > 0) {
                log.info("알림 읽음 처리 성공: notificationId={}, userId={}", notificationId, userId);
                return true;
            } else {
                log.warn("알림 읽음 처리 실패 - 해당 알림 없음: notificationId={}, userId={}", notificationId, userId);
                return false;
            }

        } catch (Exception e) {
            log.error("알림 읽음 처리 실패: notificationId={}, userId={}, error={}",
                    notificationId, userId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 사용자별 알림 통계 조회
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getUserNotificationStats(String userId) {
        try {
            long totalCount = notificationRepository.countByUserId(userId);
            long unreadCount = notificationRepository.countByUserIdAndIsReadFalse(userId);
            long readCount = totalCount - unreadCount;

            // 최근 24시간 알림 수
            LocalDateTime yesterday = LocalDateTime.now().minusHours(24);
            long recentCount = notificationRepository.countByUserIdAndCreatedAtAfter(userId, yesterday);

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalCount", totalCount);
            stats.put("unreadCount", unreadCount);
            stats.put("readCount", readCount);
            stats.put("recentCount", recentCount);
            stats.put("hasUnread", unreadCount > 0);

            log.debug("사용자 알림 통계: userId={}, stats={}", userId, stats);
            return stats;

        } catch (Exception e) {
            log.error("사용자 알림 통계 조회 실패: userId={}", userId, e);
            return Map.of(
                    "totalCount", 0L,
                    "unreadCount", 0L,
                    "readCount", 0L,
                    "recentCount", 0L,
                    "hasUnread", false
            );
        }
    }

    // ================================
    //  헤더용 메서드들 (추가)
    // ================================

    /**
     * 읽지 않은 알림 개수 조회 (헤더용)
     */
    @Transactional(readOnly = true)
    public long getUnreadCountByUserId(String userId) {
        return getUnreadCount(userId); // 기존 메서드 재사용
    }

    /**
     * 최근 알림 목록 조회 (헤더용)
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getRecentNotificationsByUserId(String userId, int limit) {
        try {
            // 기존 Repository 메서드 활용
            List<LiveBroadcastNotification> notifications =
                    notificationRepository.findByUserIdOrderByCreatedAtDesc(userId,
                            PageRequest.of(0, limit)).getContent();

            // Map 형태로 변환 (프론트엔드 호환성)
            return notifications.stream()
                    .map(this::convertToHeaderMap)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("최근 알림 조회 실패: userId={}", userId, e);
            return Collections.emptyList();
        }
    }

    /**
     * 모든 알림 읽음 처리 (헤더용)
     */
    public int markAllAsReadByUserId(String userId) {
        return markAllAsRead(userId); // 기존 메서드 재사용
    }

    /**
     *  특정 알림 읽음 처리 (헤더용)
     */
    public boolean markAsReadByNotificationId(Long notificationId, String userId) {
        return markAsRead(notificationId, userId); // 기존 메서드 재사용
    }

    /**
     *  Entity를 헤더용 Map으로 변환
     */
    private Map<String, Object> convertToHeaderMap(LiveBroadcastNotification notification) {
        Map<String, Object> map = new HashMap<>();
        map.put("notificationId", notification.getNotificationId());
        map.put("broadcastId", notification.getBroadcastId());
        map.put("type", notification.getType());
        map.put("title", notification.getTitle());
        map.put("message", notification.getMessage());
        map.put("isRead", notification.getIsRead());
        map.put("priority", notification.getPriority());
        map.put("createdAt", notification.getCreatedAt());
        map.put("readAt", notification.getReadAt());

        return map;
    }

    /**
     * Entity를 ResponseDto로 변환
     */
    private NotificationResponseDto convertToResponseDto(LiveBroadcastNotification notification) {
        return NotificationResponseDto.builder()
                .notificationId(notification.getNotificationId())
                .broadcastId(notification.getBroadcastId())
                .userId(notification.getUserId())
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .priority(notification.getPriority())
                .isSent(notification.getIsSent())
                .sentAt(notification.getSentAt())
                .isRead(notification.getIsRead())
                .readAt(notification.getReadAt())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}