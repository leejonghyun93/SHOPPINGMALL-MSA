package org.kosa.livestreamingservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kosa.livestreamingservice.client.BroadcastServiceClient;
import org.kosa.livestreamingservice.dto.NotificationResponseDto;
import org.kosa.livestreamingservice.entity.LiveBroadcastNotification;
import org.kosa.livestreamingservice.repository.LiveBroadcastNotificationRepository;
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
    private final EmailService emailService;
    private final UserEmailService userEmailService;

    /**
     * 방송 시작 알림 구독 신청
     */
    public NotificationResponseDto subscribeBroadcastStart(String userId, Long broadcastId) {
        log.info("방송 시작 알림 구독 신청: userId={}, broadcastId={}", userId, broadcastId);

        // 1. 중복 구독 체크
        if (notificationRepository.existsByUserIdAndBroadcastIdAndType(userId, broadcastId, "BROADCAST_START")) {
            throw new IllegalArgumentException("이미 구독 중인 방송입니다.");
        }

        // 2. 방송 정보 조회 (실제 방송자 이름 포함)
        BroadcastServiceClient.BroadcastInfo broadcastInfo =
                broadcastServiceClient.getBroadcastInfo(broadcastId);

        if (broadcastInfo == null) {
            throw new IllegalArgumentException("존재하지 않는 방송입니다: " + broadcastId);
        }

        // 3. 방송자 이름 확인 로그
        log.info("방송 정보 조회 완료: broadcastId={}, title={}, broadcasterName={}, hostUserId={}",
                broadcastId, broadcastInfo.title, broadcastInfo.broadcasterName, broadcastInfo.hostUserId);

        // 4. 실제 방송자 이름으로 알림 메시지 생성
        String broadcasterName = broadcastInfo.broadcasterName != null ?
                broadcastInfo.broadcasterName : "방송자";

        // 5. 알림 엔티티 생성
        LiveBroadcastNotification notification = LiveBroadcastNotification.builder()
                .broadcastId(broadcastId)
                .userId(userId)
                .type("BROADCAST_START")
                .title(broadcastInfo.title + " 방송 시작 알림")
                .message(String.format("%s님의 방송이 시작되면 알려드릴게요!", broadcasterName))  // 🔥 실제 이름 사용
                .priority("HIGH")
                .isSent(false)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        // 6. DB 저장
        LiveBroadcastNotification saved = notificationRepository.save(notification);

        log.info("방송 시작 알림 구독 완료: notificationId={}, userId={}, broadcastId={}, broadcasterName={}",
                saved.getNotificationId(), userId, broadcastId, broadcasterName);

        return convertToResponseDto(saved);
    }

    /**
     * ❌ 방송 알림 구독 취소
     */
    public void unsubscribeBroadcast(String userId, Long broadcastId, String type) {
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
    public List<NotificationResponseDto> getUserSubscriptions(String userId) {
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
        log.info("방송 시작 알림 처리: broadcastId={}", broadcastId);

        try {
            // 1. 방송 정보 조회 (실제 방송자 이름 포함)
            BroadcastServiceClient.BroadcastInfo broadcastInfo =
                    broadcastServiceClient.getBroadcastInfo(broadcastId);

            if (broadcastInfo == null) {
                log.warn("방송 정보를 찾을 수 없습니다: broadcastId={}", broadcastId);
                return new ArrayList<>();
            }

            log.info("방송 시작 알림 처리 - 방송 정보: broadcastId={}, title={}, broadcasterName={}",
                    broadcastId, broadcastInfo.title, broadcastInfo.broadcasterName);

            // 2. 해당 방송의 구독자들 조회
            List<LiveBroadcastNotification> notifications =
                    notificationRepository.findByBroadcastIdAndIsSentFalse(broadcastId);

            log.info("방송 구독자 수: broadcastId={}, subscriberCount={}", broadcastId, notifications.size());

            // 3. 구독자들에게 이메일 발송
            String broadcasterName = broadcastInfo.broadcasterName != null ?
                    broadcastInfo.broadcasterName : "방송자";

            for (LiveBroadcastNotification notification : notifications) {
                try {
                    String userEmail = userEmailService.getUserEmail(notification.getUserId());

                    if (userEmail != null) {
                        // 🔥 실제 방송자 이름으로 이메일 발송
                        emailService.sendBroadcastStartNotification(
                                userEmail,
                                notification.getUserId(),
                                broadcastInfo.title,
                                broadcasterName,  // 실제 방송자 이름 사용
                                broadcastId
                        );

                        // 발송 완료 처리
                        notification.setIsSent(true);
                        notification.setSentAt(LocalDateTime.now());

                        log.info("방송 시작 알림 이메일 발송 완료: userId={}, broadcasterName={}",
                                notification.getUserId(), broadcasterName);
                    } else {
                        log.warn("사용자 이메일을 찾을 수 없습니다: userId={}", notification.getUserId());
                    }
                } catch (Exception e) {
                    log.error("이메일 발송 실패: userId={}, error={}",
                            notification.getUserId(), e.getMessage());
                }
            }

            // 4. 알림 상태 일괄 업데이트
            notificationRepository.saveAll(notifications);

            log.info("방송 시작 알림 처리 완료: broadcastId={}, 발송완료={}/{}",
                    broadcastId,
                    notifications.stream().mapToInt(n -> n.getIsSent() ? 1 : 0).sum(),
                    notifications.size());

            return notifications.stream()
                    .map(this::convertToResponseDto)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("방송 알림 처리 실패: broadcastId={}, error={}", broadcastId, e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 📊 사용자의 모든 알림 목록 조회 (읽음/안읽음 포함)
     */
    @Transactional(readOnly = true)
    public List<NotificationResponseDto> getAllUserNotifications(String userId) {
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
     * 🔥 방송자 정보 갱신 (방송 정보 변경시)
     * 기존 알림 메시지의 방송자 이름을 업데이트
     */
    @Transactional
    public void updateBroadcasterInfo(Long broadcastId) {
        log.info("방송자 정보 갱신: broadcastId={}", broadcastId);

        try {
            // 1. 최신 방송 정보 조회
            BroadcastServiceClient.BroadcastInfo broadcastInfo =
                    broadcastServiceClient.getBroadcastInfo(broadcastId);

            if (broadcastInfo == null) {
                log.warn("방송 정보를 찾을 수 없습니다: broadcastId={}", broadcastId);
                return;
            }

            // 2. 해당 방송의 미발송 알림들 조회
            List<LiveBroadcastNotification> notifications =
                    notificationRepository.findByBroadcastIdAndIsSentFalse(broadcastId);

            // 3. 알림 메시지 업데이트
            String broadcasterName = broadcastInfo.broadcasterName != null ?
                    broadcastInfo.broadcasterName : "방송자";

            for (LiveBroadcastNotification notification : notifications) {
                String updatedMessage = String.format("%s님의 방송이 시작되면 알려드릴게요!", broadcasterName);
                notification.setMessage(updatedMessage);
                notification.setTitle(broadcastInfo.title + " 방송 시작 알림");
            }

            // 4. 일괄 업데이트
            notificationRepository.saveAll(notifications);

            log.info("방송자 정보 갱신 완료: broadcastId={}, updatedCount={}, broadcasterName={}",
                    broadcastId, notifications.size(), broadcasterName);

        } catch (Exception e) {
            log.error("방송자 정보 갱신 실패: broadcastId={}, error={}", broadcastId, e.getMessage());
        }
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