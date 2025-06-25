package org.kosa.notificationservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kosa.notificationservice.client.BroadcastServiceClient;
import org.kosa.notificationservice.dto.NotificationResponseDto;
import org.kosa.notificationservice.entity.LiveBroadcastNotification;
import org.kosa.notificationservice.repository.LiveBroadcastNotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * 방송 알림 구독 관리 서비스
 * 사용자가 특정 방송에 알림을 신청/취소하는 기능
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class NotificationSubscriptionService {

    private final LiveBroadcastNotificationRepository notificationRepository;
    private final BroadcastServiceClient broadcastServiceClient;

    /**
     * 🔔 방송 시작 알림 구독 신청
     */
    public NotificationResponseDto subscribeBroadcastStart(Long userId, Long broadcastId) {
        log.info("방송 시작 알림 구독 신청: userId={}, broadcastId={}", userId, broadcastId);

        // 1. 중복 구독 체크
        if (notificationRepository.existsByUserIdAndBroadcastIdAndType(userId, broadcastId, "BROADCAST_START")) {
            throw new IllegalArgumentException("이미 구독 중인 방송입니다.");
        }

        // 2. 방송 정보 조회
        BroadcastServiceClient.BroadcastInfo broadcastInfo =
                broadcastServiceClient.getBroadcastInfo(broadcastId);

        if (broadcastInfo == null) {
            throw new IllegalArgumentException("존재하지 않는 방송입니다: " + broadcastId);
        }

        // 3. 알림 엔티티 생성
        LiveBroadcastNotification notification = LiveBroadcastNotification.builder()
                .broadcastId(broadcastId)
                .userId(userId)
                .type("BROADCAST_START")
                .title(broadcastInfo.title + " 방송 시작 알림")
                .message(broadcastInfo.broadcasterName + "님의 방송이 시작되면 알려드릴게요!")
                .priority("HIGH")
                .isSent(false)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        // 4. DB 저장
        LiveBroadcastNotification saved = notificationRepository.save(notification);

        log.info("방송 시작 알림 구독 완료: notificationId={}, userId={}, broadcastId={}",
                saved.getNotificationId(), userId, broadcastId);

        return convertToResponseDto(saved);
    }

    /**
     * ❌ 방송 알림 구독 취소
     */
    public void unsubscribeBroadcast(Long userId, Long broadcastId, String type) {
        log.info("방송 알림 구독 취소: userId={}, broadcastId={}, type={}", userId, broadcastId, type);

        // 구독 중인 알림이 있는지 먼저 확인
        boolean exists = notificationRepository.existsByUserIdAndBroadcastIdAndType(userId, broadcastId, type);

        if (!exists) {
            throw new IllegalArgumentException("구독 중인 알림이 없습니다.");
        }

        // 알림 삭제
        notificationRepository.deleteByUserIdAndBroadcastIdAndType(userId, broadcastId, type);

        log.info("방송 알림 구독 취소 완료: userId={}, broadcastId={}, type={}", userId, broadcastId, type);
    }

    /**
     * 📋 사용자의 구독 중인 방송 목록 조회
     */
    @Transactional(readOnly = true)
    public List<NotificationResponseDto> getUserSubscriptions(Long userId) {
        log.info("사용자 구독 목록 조회: userId={}", userId);

        // 사용자의 미발송 알림들 조회 (구독 중인 것들)
        List<LiveBroadcastNotification> notifications =
                notificationRepository.findByUserIdAndIsSentFalseOrderByCreatedAtDesc(userId);

        log.info("사용자 구독 목록 조회 완료: userId={}, count={}", userId, notifications.size());

        return notifications.stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * 🎯 특정 방송의 구독자 수 조회
     */
    @Transactional(readOnly = true)
    public long getBroadcastSubscriberCount(Long broadcastId) {
        long count = notificationRepository.countByBroadcastIdAndTypeAndIsSentFalse(broadcastId, "BROADCAST_START");

        log.info("방송 구독자 수 조회: broadcastId={}, count={}", broadcastId, count);

        return count;
    }

    /**
     * 🔥 방송 시작시 구독자들에게 대량 알림 생성 (Live Streaming Service에서 호출)
     */
    public List<NotificationResponseDto> createBroadcastStartNotifications(Long broadcastId) {
        log.info("방송 시작 - 구독자들에게 알림 생성: broadcastId={}", broadcastId);

        try {
            // 1. 방송 정보 조회
            BroadcastServiceClient.BroadcastInfo broadcastInfo =
                    broadcastServiceClient.getBroadcastInfo(broadcastId);

            if (broadcastInfo == null) {
                log.warn("방송 정보를 찾을 수 없습니다: broadcastId={}", broadcastId);
                return new ArrayList<>();
            }

            // 2. 해당 방송을 구독 중인 미발송 알림들 조회
            List<LiveBroadcastNotification> existingNotifications =
                    notificationRepository.findByBroadcastIdAndIsSentFalse(broadcastId);

            if (existingNotifications.isEmpty()) {
                log.info("구독자가 없습니다: broadcastId={}", broadcastId);
                return new ArrayList<>();
            }

            // 3. 알림 제목/메시지 업데이트 (방송 시작에 맞게)
            for (LiveBroadcastNotification notification : existingNotifications) {
                notification.setTitle(broadcastInfo.title + " - 방송이 시작되었습니다!");
                notification.setMessage(String.format(
                        "🔴 %s님의 라이브 방송이 지금 시작되었습니다!\n" +
                                "📺 방송 제목: %s\n" +
                                "⏰ 시작 시간: %s\n\n" +
                                "놓치지 마시고 지금 바로 시청하세요!",
                        broadcastInfo.broadcasterName,
                        broadcastInfo.title,
                        broadcastInfo.scheduledStartTime
                ));
                notification.setPriority("URGENT"); // 방송 시작시에는 긴급으로 변경
            }

            // 4. 업데이트된 알림들 저장
            List<LiveBroadcastNotification> updatedNotifications =
                    notificationRepository.saveAll(existingNotifications);

            log.info("방송 시작 알림 생성 완료: broadcastId={}, subscriberCount={}",
                    broadcastId, updatedNotifications.size());

            // 5. ResponseDto로 변환하여 반환
            return updatedNotifications.stream()
                    .map(this::convertToResponseDto)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("방송 시작 알림 생성 실패: broadcastId={}, error={}", broadcastId, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * 📊 사용자의 모든 알림 목록 조회 (읽음/안읽음 포함)
     */
    @Transactional(readOnly = true)
    public List<NotificationResponseDto> getAllUserNotifications(Long userId) {
        log.info("사용자 전체 알림 목록 조회: userId={}", userId);

        List<LiveBroadcastNotification> notifications =
                notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);

        return notifications.stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * 🔍 특정 방송의 구독자 목록 조회 (관리자용)
     */
    @Transactional(readOnly = true)
    public List<NotificationResponseDto> getBroadcastSubscribers(Long broadcastId) {
        log.info("방송 구독자 목록 조회: broadcastId={}", broadcastId);

        List<LiveBroadcastNotification> notifications =
                notificationRepository.findByBroadcastIdAndIsSentFalse(broadcastId);

        return notifications.stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * 🧹 만료된 구독 정리 (방송이 끝난 후)
     */
    @Transactional
    public int cleanupExpiredSubscriptions(Long broadcastId) {
        log.info("만료된 구독 정리: broadcastId={}", broadcastId);

        List<LiveBroadcastNotification> expiredNotifications =
                notificationRepository.findByBroadcastIdAndIsSentFalse(broadcastId);

        if (!expiredNotifications.isEmpty()) {
            notificationRepository.deleteAll(expiredNotifications);
            log.info("만료된 구독 정리 완료: broadcastId={}, count={}", broadcastId, expiredNotifications.size());
        }

        return expiredNotifications.size();
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