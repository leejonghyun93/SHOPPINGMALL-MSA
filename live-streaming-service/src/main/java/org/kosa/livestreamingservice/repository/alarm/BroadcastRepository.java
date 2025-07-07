package org.kosa.livestreamingservice.repository.alarm;

import org.kosa.livestreamingservice.entity.alarm.BroadcastEntity;
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
     *  특정 시간 범위에서 시작하는 방송들 조회 (상태별)
     */
    List<BroadcastEntity> findByScheduledStartTimeBetweenAndBroadcastStatus(
            LocalDateTime startTime,
            LocalDateTime endTime,
            String broadcastStatus
    );

    /**
     *  특정 시간 범위에서 시작하는 모든 방송들 조회
     */
    List<BroadcastEntity> findByScheduledStartTimeBetween(
            LocalDateTime startTime,
            LocalDateTime endTime
    );

    /**
     *  방송 상태별 조회
     */
    List<BroadcastEntity> findByBroadcastStatus(String broadcastStatus);

    /**
     *  방송 상태별 조회 (정렬)
     */
    List<BroadcastEntity> findByBroadcastStatusOrderByScheduledStartTimeAsc(String broadcastStatus);

    /**
     *  현재 시간 기준으로 시작해야 할 방송들 조회 (더 정확한 쿼리)
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
     *  정확히 특정 시간에 시작하는 방송들 조회 (분 단위)
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
     *  특정 날짜의 예약된 방송들 조회
     */
    @Query("SELECT b FROM BroadcastEntity b WHERE " +
            "DATE(b.scheduledStartTime) = :date " +
            "AND b.broadcastStatus = 'scheduled' " +
            "ORDER BY b.scheduledStartTime ASC")
    List<BroadcastEntity> findScheduledBroadcastsByDate(@Param("date") LocalDate date);

    /**
     *  현재 라이브 중인 방송들 조회
     */
    List<BroadcastEntity> findByBroadcastStatusIn(List<String> statuses);

    /**
     *  특정 시간 이후 시작하는 예약된 방송들 조회
     */
    @Query("SELECT b FROM BroadcastEntity b WHERE " +
            "b.scheduledStartTime >= :fromTime " +
            "AND b.broadcastStatus = 'scheduled' " +
            "ORDER BY b.scheduledStartTime ASC")
    List<BroadcastEntity> findUpcomingScheduledBroadcasts(@Param("fromTime") LocalDateTime fromTime);

    /**
     *  오늘 예약된 방송 개수 조회
     */
    @Query("SELECT COUNT(b) FROM BroadcastEntity b WHERE " +
            "DATE(b.scheduledStartTime) = CURRENT_DATE " +
            "AND b.broadcastStatus = 'scheduled'")
    long countTodayScheduledBroadcasts();

    // ========== 🔥 실제 운영용: broadcaster_id 관련 메서드들 (Long과 String 둘 다 지원) ==========

    /**
     *  broadcaster_id만 조회 (String 반환)
     */
    @Query("SELECT b.broadcasterId FROM BroadcastEntity b WHERE b.broadcastId = :broadcastId")
    Optional<String> findBroadcasterIdByBroadcastId(@Param("broadcastId") Long broadcastId);

    /**
     *  특정 broadcaster_id의 방송 목록 조회 (String 타입)
     */
    List<BroadcastEntity> findByBroadcasterIdOrderByScheduledStartTimeDesc(String broadcasterId);

    /**
     * 특정 broadcaster_id의 활성 방송 조회 (String 타입)
     */
    @Query("SELECT b FROM BroadcastEntity b WHERE " +
            "b.broadcasterId = :broadcasterId " +
            "AND b.broadcastStatus IN ('scheduled', 'live', 'starting') " +
            "ORDER BY b.scheduledStartTime ASC")
    List<BroadcastEntity> findActiveBroadcastsByBroadcasterId(@Param("broadcasterId") String broadcasterId);

    /**
     *  broadcaster_id별 방송 개수 조회 (String 타입)
     */
    long countByBroadcasterId(String broadcasterId);

    /**
     *  현재 시작하는 방송들의 broadcaster_id 목록 조회 (String 반환)
     */
    @Query("SELECT DISTINCT b.broadcasterId FROM BroadcastEntity b WHERE " +
            "b.scheduledStartTime BETWEEN :startTime AND :endTime " +
            "AND b.broadcastStatus = 'scheduled'")
    List<String> findStartingBroadcasterIds(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     *  카테고리별 방송 조회
     */
    List<BroadcastEntity> findByCategoryIdAndBroadcastStatusOrderByScheduledStartTimeAsc(
            Integer categoryId, String broadcastStatus);

    /**
     *  공개 방송만 조회
     */
    List<BroadcastEntity> findByIsPublicTrueAndBroadcastStatusOrderByScheduledStartTimeAsc(String broadcastStatus);

    /**
     *  현재 라이브 중인 방송 조회 (시청자 수 내림차순)
     */
    @Query("SELECT b FROM BroadcastEntity b WHERE b.broadcastStatus = 'live' ORDER BY b.currentViewers DESC")
    List<BroadcastEntity> findCurrentLiveBroadcasts();

    /**
     *  방송자의 활성 방송 수 조회 (String 타입)
     */
    @Query("SELECT COUNT(b) FROM BroadcastEntity b WHERE b.broadcasterId = :broadcasterId AND b.broadcastStatus IN ('scheduled', 'live', 'starting')")
    long countActiveBroadcastsByBroadcasterId(@Param("broadcasterId") String broadcasterId);

    /**
     *  특정 방송의 상세 정보 조회 (JOIN 없이)
     */
    @Query("SELECT b FROM BroadcastEntity b WHERE b.broadcastId = :broadcastId AND b.isPublic = true")
    Optional<BroadcastEntity> findPublicBroadcastById(@Param("broadcastId") Long broadcastId);

    /**
     *  인기 방송 조회 (시청자 수 기준)
     */
    @Query("SELECT b FROM BroadcastEntity b WHERE b.broadcastStatus IN ('live', 'starting') ORDER BY b.currentViewers DESC")
    List<BroadcastEntity> findPopularLiveBroadcasts();

    /**
     *  최근 종료된 방송 조회 (다시보기용)
     */
    @Query("SELECT b FROM BroadcastEntity b WHERE b.broadcastStatus = 'ended' AND b.videoUrl IS NOT NULL ORDER BY b.actualEndTime DESC")
    List<BroadcastEntity> findRecentEndedBroadcastsWithVideo();

    /**
     *  태그로 방송 검색
     */
    @Query("SELECT b FROM BroadcastEntity b WHERE b.tags LIKE %:tag% AND b.broadcastStatus IN ('live', 'scheduled') ORDER BY b.scheduledStartTime ASC")
    List<BroadcastEntity> findByTagsContaining(@Param("tag") String tag);
}