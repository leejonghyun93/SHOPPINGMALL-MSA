package org.kosa.notificationservice.repository;

import org.kosa.notificationservice.entity.LiveBroadcastNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LiveBroadcastNotificationRepository extends JpaRepository<LiveBroadcastNotification, Long> {

    // ==== 🔥 배치 처리용 쿼리들 (핵심!) ====

    /**
     * 특정 방송들의 미발송 알림들 조회 (배치에서 가장 많이 사용)
     */
    List<LiveBroadcastNotification> findByBroadcastIdInAndIsSentFalseAndType(
            List<Long> broadcastIds,
            String type
    );

    /**
     * 방송별 미발송 알림 조회
     */
    List<LiveBroadcastNotification> findByBroadcastIdAndIsSentFalse(Long broadcastId);

    /**
     * 미발송 알림 대량 업데이트
     */
    @Modifying
    @Transactional
    @Query("UPDATE LiveBroadcastNotification n " +
            "SET n.isSent = true, n.sentAt = :sentAt " +
            "WHERE n.notificationId IN :notificationIds")
    void markNotificationAsSent(
            @Param("notificationIds") List<Long> notificationIds,
            @Param("sentAt") LocalDateTime sentAt
    );

    // ========== 사용자 기능용 쿼리들 =======

    /**
     * 사용자별 알림 목록 조회 (페이징)
     */
    Page<LiveBroadcastNotification> findByUserIdOrderByCreatedAtDesc(
            Long userId,
            Pageable pageable
    );

    /**
     * 사용자별 알림 목록 조회 (전체)
     */
    List<LiveBroadcastNotification> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * 읽지 않은 알림 조회
     */
    List<LiveBroadcastNotification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(Long userId);

    /**
     * 🆕 사용자별 미발송 알림 조회 (구독 목록용)
     */
    List<LiveBroadcastNotification> findByUserIdAndIsSentFalseOrderByCreatedAtDesc(Long userId);

    /**
     * 읽지 않은 알림 개수
     */
    long countByUserIdAndIsReadFalse(Long userId);

    /**
     * 특정 사용자의 특정 방송 알림 조회 (중복 알림 방지용)
     */
    boolean existsByUserIdAndBroadcastIdAndType(Long userId, Long broadcastId, String type);

    /**
     * 읽음 처리
     */
    @Modifying
    @Transactional
    @Query("UPDATE LiveBroadcastNotification n " +
            "SET n.isRead = true, n.readAt = :readAt " +
            "WHERE n.notificationId = :notificationId AND n.userId = :userId")
    void markAsRead(
            @Param("notificationId") Long notificationId,
            @Param("userId") Long userId,
            @Param("readAt") LocalDateTime readAt
    );

    /**
     * 방송별 알림 신청자 수 (전체)
     */
    long countByBroadcastId(Long broadcastId);

    /**
     * 🆕 방송별 미발송 알림 신청자 수 (실제 구독자 수)
     */
    long countByBroadcastIdAndTypeAndIsSentFalse(Long broadcastId, String type);

    /**
     * 타입별 알림 통계
     */
    long countByTypeAndCreatedAtAfter(String type, LocalDateTime createdAt);

    /**
     * 특정 기간 알림 조회
     */
    List<LiveBroadcastNotification> findByCreatedAtBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 알림 구독 취소 (삭제)
     */
    @Modifying
    @Transactional
    void deleteByUserIdAndBroadcastIdAndType(Long userId, Long broadcastId, String type);

    // ========== 🆕 추가 유용한 쿼리들 =======

    /**
     * 특정 타입의 미발송 알림들 조회
     */
    List<LiveBroadcastNotification> findByTypeAndIsSentFalse(String type);

    /**
     * 방송별 특정 타입 알림들 조회
     */
    List<LiveBroadcastNotification> findByBroadcastIdAndType(Long broadcastId, String type);

    /**
     * 사용자별 특정 방송 알림들 조회
     */
    List<LiveBroadcastNotification> findByUserIdAndBroadcastId(Long userId, Long broadcastId);

    /**
     * 오래된 발송 완료 알림들 조회 (정리용)
     */
    @Query("SELECT n FROM LiveBroadcastNotification n " +
            "WHERE n.isSent = true AND n.sentAt < :cutoffDate")
    List<LiveBroadcastNotification> findOldSentNotifications(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * 특정 우선순위 알림들 조회
     */
    List<LiveBroadcastNotification> findByPriorityAndIsSentFalse(String priority);

    /**
     * 발송 실패 알림들 조회 (추후 재발송용)
     */
    @Query("SELECT n FROM LiveBroadcastNotification n " +
            "WHERE n.isSent = false AND n.createdAt < :cutoffTime")
    List<LiveBroadcastNotification> findFailedNotifications(@Param("cutoffTime") LocalDateTime cutoffTime);
}