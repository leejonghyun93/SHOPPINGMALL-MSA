package org.kosa.notificationservice.repository;

import org.kosa.notificationservice.entity.BroadcastEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BroadcastRepository extends JpaRepository<BroadcastEntity, Long> {

    @Query("SELECT b FROM BroadcastEntity b WHERE DATE(b.scheduledStartTime) = :date ORDER BY b.scheduledStartTime")
    List<BroadcastEntity> findByScheduledDate(@Param("date") LocalDate date);

    @Query("SELECT b FROM BroadcastEntity b WHERE b.scheduledStartTime BETWEEN :startTime AND :endTime ORDER BY b.scheduledStartTime")
    List<BroadcastEntity> findByScheduledTimeBetween(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 🔥 특정 시간 범위에서 시작하는 방송들 조회 (상태별)
     */
    List<BroadcastEntity> findByScheduledStartTimeBetweenAndBroadcastStatus(
            LocalDateTime startTime,
            LocalDateTime endTime,
            String broadcastStatus
    );

    /**
     * 🔥 특정 시간 범위에서 시작하는 모든 방송들 조회
     */
    List<BroadcastEntity> findByScheduledStartTimeBetween(
            LocalDateTime startTime,
            LocalDateTime endTime
    );

    /**
     * 🔥 방송 상태별 조회
     */
    List<BroadcastEntity> findByBroadcastStatus(String broadcastStatus);

    /**
     * 🔥 방송 상태별 조회 (정렬)
     */
    List<BroadcastEntity> findByBroadcastStatusOrderByScheduledStartTimeAsc(String broadcastStatus);

    /**
     * 🔥 현재 시간 기준으로 시작해야 할 방송들 조회 (더 정확한 쿼리)
     */
    @Query("SELECT b FROM BroadcastEntity b WHERE " +
            "b.scheduledStartTime BETWEEN :startTime AND :endTime " +
            "AND b.broadcastStatus = 'scheduled' " +
            "ORDER BY b.scheduledStartTime ASC")
    List<BroadcastEntity> findBroadcastsStartingBetween(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * 🔥 정확히 특정 시간에 시작하는 방송들 조회 (분 단위)
     */
    @Query("SELECT b FROM BroadcastEntity b WHERE " +
            "HOUR(b.scheduledStartTime) = :hour " +
            "AND MINUTE(b.scheduledStartTime) = :minute " +
            "AND DATE(b.scheduledStartTime) = DATE(:currentTime) " +
            "AND b.broadcastStatus = 'scheduled'")
    List<BroadcastEntity> findBroadcastsStartingAtTime(
            @Param("hour") int hour,
            @Param("minute") int minute,
            @Param("currentTime") LocalDateTime currentTime
    );

    /**
     * 🔥 특정 날짜의 예약된 방송들 조회
     */
    @Query("SELECT b FROM BroadcastEntity b WHERE " +
            "DATE(b.scheduledStartTime) = :date " +
            "AND b.broadcastStatus = 'scheduled' " +
            "ORDER BY b.scheduledStartTime ASC")
    List<BroadcastEntity> findScheduledBroadcastsByDate(@Param("date") LocalDate date);

    /**
     * 🔥 현재 라이브 중인 방송들 조회
     */
    List<BroadcastEntity> findByBroadcastStatusIn(List<String> statuses);

    /**
     * 🔥 특정 시간 이후 시작하는 예약된 방송들 조회
     */
    @Query("SELECT b FROM BroadcastEntity b WHERE " +
            "b.scheduledStartTime >= :fromTime " +
            "AND b.broadcastStatus = 'scheduled' " +
            "ORDER BY b.scheduledStartTime ASC")
    List<BroadcastEntity> findUpcomingScheduledBroadcasts(@Param("fromTime") LocalDateTime fromTime);

    /**
     * 🔥 오늘 예약된 방송 개수 조회
     */
    @Query("SELECT COUNT(b) FROM BroadcastEntity b WHERE " +
            "DATE(b.scheduledStartTime) = CURRENT_DATE " +
            "AND b.broadcastStatus = 'scheduled'")
    long countTodayScheduledBroadcasts();

    // ========== 🔥 새로 추가: 방송자 정보 조회 메서드들 ==========

    /**
     * 🔥 방송 정보와 방송자 이름을 함께 조회하는 메서드
     * tb_live_broadcasts와 tb_member를 JOIN하여 실제 방송자 이름 조회
     */
    @Query(value = """
        SELECT b.broadcast_id as broadcastId,
               b.title as title,
               b.description as description,
               b.broadcast_status as broadcastStatus,
               b.scheduled_start_time as scheduledStartTime,
               b.broadcaster_id as broadcasterId,
               m.USER_ID as broadcasterUserId,
               m.NAME as broadcasterName,
               m.EMAIL as broadcasterEmail
        FROM tb_live_broadcasts b
        LEFT JOIN tb_member m ON CAST(b.broadcaster_id AS CHAR) = m.USER_ID
        WHERE b.broadcast_id = :broadcastId
        """, nativeQuery = true)
    Optional<BroadcastWithMemberInfo> findBroadcastWithMemberInfo(@Param("broadcastId") Long broadcastId);

    /**
     * 🔥 현재 시작하는 방송들과 방송자 정보를 함께 조회
     */
    @Query(value = """
        SELECT b.broadcast_id as broadcastId,
               b.title as title,
               b.description as description,
               b.broadcast_status as broadcastStatus,
               b.scheduled_start_time as scheduledStartTime,
               b.broadcaster_id as broadcasterId,
               m.USER_ID as broadcasterUserId,
               m.NAME as broadcasterName,
               m.EMAIL as broadcasterEmail
        FROM tb_live_broadcasts b
        LEFT JOIN tb_member m ON CAST(b.broadcaster_id AS CHAR) = m.USER_ID
        WHERE b.scheduled_start_time BETWEEN :startTime AND :endTime
        AND b.broadcast_status = :status
        ORDER BY b.scheduled_start_time
        """, nativeQuery = true)
    List<BroadcastWithMemberInfo> findStartingBroadcastsWithMemberInfo(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("status") String status);

    /**
     * 🔥 특정 방송자의 방송 목록 조회
     */
    @Query(value = """
        SELECT b.broadcast_id as broadcastId,
               b.title as title,
               b.description as description,
               b.broadcast_status as broadcastStatus,
               b.scheduled_start_time as scheduledStartTime,
               b.broadcaster_id as broadcasterId,
               m.USER_ID as broadcasterUserId,
               m.NAME as broadcasterName,
               m.EMAIL as broadcasterEmail
        FROM tb_live_broadcasts b
        LEFT JOIN tb_member m ON CAST(b.broadcaster_id AS CHAR) = m.USER_ID
        WHERE m.USER_ID = :userId
        ORDER BY b.scheduled_start_time DESC
        """, nativeQuery = true)
    List<BroadcastWithMemberInfo> findBroadcastsByBroadcasterUserId(@Param("userId") String userId);

    /**
     * 🔥 방송자 이름만 조회하는 간단한 메서드
     */
    @Query(value = """
        SELECT m.NAME
        FROM tb_live_broadcasts b
        LEFT JOIN tb_member m ON CAST(b.broadcaster_id AS CHAR) = m.USER_ID
        WHERE b.broadcast_id = :broadcastId
        """, nativeQuery = true)
    Optional<String> findBroadcasterNameByBroadcastId(@Param("broadcastId") Long broadcastId);

    /**
     * 🔥 broadcaster_id로 USER_ID 조회
     */
    @Query(value = """
        SELECT m.USER_ID
        FROM tb_live_broadcasts b
        LEFT JOIN tb_member m ON CAST(b.broadcaster_id AS CHAR) = m.USER_ID
        WHERE b.broadcast_id = :broadcastId
        """, nativeQuery = true)
    Optional<String> findBroadcasterUserIdByBroadcastId(@Param("broadcastId") Long broadcastId);

    /**
     * 🔥 방송자 이메일 조회
     */
    @Query(value = """
        SELECT m.EMAIL
        FROM tb_live_broadcasts b
        LEFT JOIN tb_member m ON CAST(b.broadcaster_id AS CHAR) = m.USER_ID
        WHERE b.broadcast_id = :broadcastId
        """, nativeQuery = true)
    Optional<String> findBroadcasterEmailByBroadcastId(@Param("broadcastId") Long broadcastId);

    /**
     * 🔥 방송자 정보와 함께 스케줄 조회 (특정 날짜)
     */
    @Query(value = """
        SELECT b.broadcast_id as broadcastId,
               b.title as title,
               b.description as description,
               b.broadcast_status as broadcastStatus,
               b.scheduled_start_time as scheduledStartTime,
               b.broadcaster_id as broadcasterId,
               m.USER_ID as broadcasterUserId,
               m.NAME as broadcasterName,
               m.EMAIL as broadcasterEmail
        FROM tb_live_broadcasts b
        LEFT JOIN tb_member m ON CAST(b.broadcaster_id AS CHAR) = m.USER_ID
        WHERE DATE(b.scheduled_start_time) = :date
        AND b.broadcast_status = 'scheduled'
        ORDER BY b.scheduled_start_time ASC
        """, nativeQuery = true)
    List<BroadcastWithMemberInfo> findScheduledBroadcastsWithMemberInfoByDate(@Param("date") LocalDate date);

    /**
     * 방송 정보와 회원 정보를 담는 Projection 인터페이스
     */
    interface BroadcastWithMemberInfo {
        Long getBroadcastId();
        String getTitle();
        String getDescription();
        String getBroadcastStatus();
        LocalDateTime getScheduledStartTime();
        Long getBroadcasterId();
        String getBroadcasterUserId();  // tb_member.USER_ID
        String getBroadcasterName();    // tb_member.NAME
        String getBroadcasterEmail();   // tb_member.EMAIL
    }

    /**
     * 🔥 broadcaster_id별 방송 개수 조회 (통계용)
     */
    @Query(value = """
        SELECT COUNT(*)
        FROM tb_live_broadcasts b
        WHERE b.broadcaster_id = :broadcasterId
        """, nativeQuery = true)
    long countByBroadcasterId(@Param("broadcasterId") Long broadcasterId);

    /**
     * 🔥 특정 방송자의 활성 방송 조회
     */
    @Query(value = """
        SELECT b.broadcast_id as broadcastId,
               b.title as title,
               b.description as description,
               b.broadcast_status as broadcastStatus,
               b.scheduled_start_time as scheduledStartTime,
               b.broadcaster_id as broadcasterId,
               m.USER_ID as broadcasterUserId,
               m.NAME as broadcasterName,
               m.EMAIL as broadcasterEmail
        FROM tb_live_broadcasts b
        LEFT JOIN tb_member m ON CAST(b.broadcaster_id AS CHAR) = m.USER_ID
        WHERE m.USER_ID = :userId
        AND b.broadcast_status IN ('scheduled', 'live', 'starting')
        ORDER BY b.scheduled_start_time ASC
        """, nativeQuery = true)
    List<BroadcastWithMemberInfo> findActiveBroadcastsByBroadcasterUserId(@Param("userId") String userId);
}