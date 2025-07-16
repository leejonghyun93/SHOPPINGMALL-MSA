package org.kosa.livestreamingservice.repository.alarm;

import org.kosa.livestreamingservice.entity.alarm.LiveBroadcastNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface LiveBroadcastNotificationRepository extends JpaRepository<LiveBroadcastNotification, Long> {

    // ========== 기본 조회 메서드들 ==========

    /**
     * 사용자별 알림 조회 (페이징)
     */
    Page<LiveBroadcastNotification> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    /**
     * 사용자별 모든 알림 조회
     */
    List<LiveBroadcastNotification> findByUserIdOrderByCreatedAtDesc(String userId);

    /**
     * 사용자별 읽지 않은 알림 조회
     */
    List<LiveBroadcastNotification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(String userId);

    /**
     * 사용자별 미발송 알림 조회 (구독 중인 것들)
     */
    List<LiveBroadcastNotification> findByUserIdAndIsSentFalseOrderByCreatedAtDesc(String userId);

    // ========== 방송별 조회 메서드들 ==========

    /**
     * 방송별 모든 알림 조회
     */
    List<LiveBroadcastNotification> findByBroadcastIdOrderByCreatedAtDesc(Long broadcastId);

    /**
     * 방송별 미발송 알림 조회
     */
    List<LiveBroadcastNotification> findByBroadcastIdAndIsSentFalse(Long broadcastId);

    /**
     * 특정 방송의 특정 타입 미발송 알림 조회
     */
    List<LiveBroadcastNotification> findByBroadcastIdAndIsSentFalseAndType(Long broadcastId, String type);

    /**
     * 🔥 여러 방송의 특정 타입 미발송 알림 조회 (배치용)
     */
    @Query("SELECT n FROM LiveBroadcastNotification n WHERE " +
            "n.broadcastId IN :broadcastIds AND n.isSent = false AND n.type = :type " +
            "ORDER BY n.createdAt ASC")
    List<LiveBroadcastNotification> findByBroadcastIdInAndIsSentFalseAndType(
            @Param("broadcastIds") List<Long> broadcastIds,
            @Param("type") String type);

    // ========== 중복 체크 및 존재 확인 ==========

    /**
     * 중복 알림 체크
     */
    boolean existsByUserIdAndBroadcastIdAndType(String userId, Long broadcastId, String type);

    // ========== 개수 조회 메서드들 ==========

    /**
     * 사용자별 읽지 않은 알림 개수
     */
    long countByUserIdAndIsReadFalse(String userId);

    /**
     * 방송별 구독자 수 (미발송 알림 기준)
     */
    long countByBroadcastIdAndTypeAndIsSentFalse(Long broadcastId, String type);

    /**
     * 방송별 전체 알림 개수
     */
    long countByBroadcastId(Long broadcastId);

    /**
     * 🔥 전체 미발송 알림 개수
     */
    @Query("SELECT COUNT(n) FROM LiveBroadcastNotification n WHERE n.isSent = false")
    long countByIsSentFalse();

    /**
     * 🔥 특정 타입의 미발송 알림 개수
     */
    @Query("SELECT COUNT(n) FROM LiveBroadcastNotification n WHERE " +
            "n.type = :type AND n.isSent = false")
    long countByTypeAndIsSentFalse(@Param("type") String type);

    /**
     * 🔥 특정 방송의 미발송 알림 개수 조회
     */
    @Query("SELECT COUNT(n) FROM LiveBroadcastNotification n WHERE " +
            "n.broadcastId = :broadcastId AND n.isSent = false")
    long countByBroadcastIdAndIsSentFalse(@Param("broadcastId") Long broadcastId);

    /**
     * 특정 기간 이후 생성된 특정 타입 알림 개수
     */
    long countByTypeAndCreatedAtAfter(String type, LocalDateTime fromDate);

    /**
     * 🔥 특정 시간 이후 생성된 알림 개수 조회
     */
    @Query("SELECT COUNT(n) FROM LiveBroadcastNotification n WHERE n.createdAt > :startTime")
    long countByCreatedAtAfter(@Param("startTime") LocalDateTime startTime);

    // ========== 업데이트 메서드들 ==========

    /**
     * 알림 읽음 처리
     */
    @Modifying
    @Query("UPDATE LiveBroadcastNotification n SET n.isRead = true, n.readAt = :readAt " +
            "WHERE n.notificationId = :notificationId AND n.userId = :userId")
    int markAsRead(@Param("notificationId") Long notificationId,
                   @Param("userId") String userId,
                   @Param("readAt") LocalDateTime readAt);

    /**
     * 🔥 대량 알림 발송 완료 처리
     */
    @Modifying
    @Query("UPDATE LiveBroadcastNotification n SET n.isSent = true, n.sentAt = :sentAt " +
            "WHERE n.notificationId IN :notificationIds")
    int markNotificationAsSent(@Param("notificationIds") List<Long> notificationIds,
                               @Param("sentAt") LocalDateTime sentAt);

    /**
     * 특정 방송의 모든 알림 발송 완료 처리
     */
    @Modifying
    @Query("UPDATE LiveBroadcastNotification n SET n.isSent = true, n.sentAt = :sentAt " +
            "WHERE n.broadcastId = :broadcastId AND n.isSent = false")
    int markBroadcastNotificationsAsSent(@Param("broadcastId") Long broadcastId,
                                         @Param("sentAt") LocalDateTime sentAt);

    // ========== 삭제 메서드들 ==========

    /**
     * 구독 취소 (특정 사용자의 특정 방송 알림 삭제)
     */
    void deleteByUserIdAndBroadcastIdAndType(String userId, Long broadcastId, String type);

    /**
     * 특정 기간 범위의 알림 조회 (정리용)
     */
    List<LiveBroadcastNotification> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    // ========== 통계 및 관리용 메서드들 ==========

    /**
     * 🔥 특정 사용자의 특정 방송 알림 조회
     */
    List<LiveBroadcastNotification> findByUserIdAndBroadcastId(String userId, Long broadcastId);

    /**
     * 🔥 특정 사용자의 특정 방송 특정 타입 알림 조회
     */
    List<LiveBroadcastNotification> findByUserIdAndBroadcastIdAndType(String userId, Long broadcastId, String type);

    /**
     * 발송 실패한 알림들 조회 (재시도용)
     */
    @Query("SELECT n FROM LiveBroadcastNotification n WHERE " +
            "n.isSent = false AND n.createdAt < :cutoffTime")
    List<LiveBroadcastNotification> findFailedNotifications(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * 🔥 오늘 발송된 알림 통계
     */
    @Query("SELECT COUNT(n) FROM LiveBroadcastNotification n WHERE " +
            "DATE(n.sentAt) = CURRENT_DATE AND n.isSent = true")
    long countTodaySentNotifications();

    /**
     * 🔥 특정 방송의 구독자 목록 조회 (사용자 ID만)
     */
    @Query("SELECT DISTINCT n.userId FROM LiveBroadcastNotification n WHERE " +
            "n.broadcastId = :broadcastId AND n.isSent = false AND n.type = 'BROADCAST_START'")
    List<String> findSubscriberUserIds(@Param("broadcastId") Long broadcastId);

    // ========== 🔥 헤더 알림용 추가 메서드들 ==========

    /**
     * 🔥 사용자별 전체 알림 개수
     */
    long countByUserId(String userId);

    /**
     * 🔥 사용자별 특정 시간 이후 알림 개수 (최근 알림 통계용)
     */
    long countByUserIdAndCreatedAtAfter(String userId, LocalDateTime fromDate);

    /**
     * 🔥 특정 사용자의 최근 알림 조회 (limit 적용)
     */
    @Query("SELECT n FROM LiveBroadcastNotification n WHERE n.userId = :userId " +
            "ORDER BY n.createdAt DESC")
    List<LiveBroadcastNotification> findRecentNotificationsByUserId(
            @Param("userId") String userId,
            Pageable pageable
    );

    /**
     * 사용자별 모든 읽지 않은 알림 읽음 처리 (bulk update)
     */
    @Modifying
    @Query("UPDATE LiveBroadcastNotification n SET n.isRead = true, n.readAt = :readAt " +
            "WHERE n.userId = :userId AND n.isRead = false")
    int markAllAsReadByUserId(@Param("userId") String userId, @Param("readAt") LocalDateTime readAt);

    /**
     * 특정 알림이 해당 사용자의 것인지 확인
     */
    boolean existsByNotificationIdAndUserId(Long notificationId, String userId);

    /**
     * 🔥 사용자별 알림 타입별 개수 조회
     */
    @Query("SELECT n.type, COUNT(n) FROM LiveBroadcastNotification n " +
            "WHERE n.userId = :userId GROUP BY n.type")
    List<Object[]> countByUserIdGroupByType(@Param("userId") String userId);

    /**
     * 🔥 사용자별 최근 N개 알림 조회 (읽음/안읽음 구분)
     */
    @Query("SELECT n FROM LiveBroadcastNotification n WHERE n.userId = :userId " +
            "ORDER BY n.isRead ASC, n.createdAt DESC")
    List<LiveBroadcastNotification> findRecentNotificationsWithUnreadFirst(
            @Param("userId") String userId,
            Pageable pageable
    );

    /**
     * 🔥 특정 시간 범위의 사용자 알림 조회 (통계용)
     */
    @Query("SELECT n FROM LiveBroadcastNotification n WHERE n.userId = :userId " +
            "AND n.createdAt BETWEEN :startDate AND :endDate " +
            "ORDER BY n.createdAt DESC")
    List<LiveBroadcastNotification> findByUserIdAndDateRange(
            @Param("userId") String userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * 사용자의 가장 최근 알림 조회
     */
    @Query("SELECT n FROM LiveBroadcastNotification n WHERE n.userId = :userId " +
            "ORDER BY n.createdAt DESC LIMIT 1")
    Optional<LiveBroadcastNotification> findLatestNotificationByUserId(@Param("userId") String userId);
}