package org.kosa.notificationservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kosa.notificationservice.dto.BroadcastDto;
import org.kosa.notificationservice.dto.BroadcastScheduleDto;
import org.kosa.notificationservice.dto.NotificationCreateDto;
import org.kosa.notificationservice.dto.NotificationResponseDto;
import org.kosa.notificationservice.entity.BroadcastEntity;
import org.kosa.notificationservice.entity.LiveBroadcastNotification;
import org.kosa.notificationservice.repository.BroadcastRepository;
import org.kosa.notificationservice.repository.LiveBroadcastNotificationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
     * 알림 읽음 처리
     */
    public void markAsRead(Long notificationId, String userId) {
        notificationRepository.markAsRead(notificationId, userId, LocalDateTime.now());
        log.info("알림 읽음 처리: notificationId={}, userId={}", notificationId, userId);
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