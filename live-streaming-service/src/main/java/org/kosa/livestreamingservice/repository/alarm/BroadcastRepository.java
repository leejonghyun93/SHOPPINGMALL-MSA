package org.kosa.livestreamingservice.repository.alarm;

import org.kosa.livestreamingservice.entity.BroadcastEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BroadcastRepository extends JpaRepository<BroadcastEntity, Long> {

    // ============ 핵심 알림 서비스용 메서드들 ============

    /**
     * 특정 날짜의 방송 스케줄 조회 (기본)
     */
    @Query("SELECT b FROM BroadcastEntity b WHERE " +
            "b.scheduledStartTime >= :startOfDay AND b.scheduledStartTime < :nextDay " +
            "AND b.broadcastStatus != 'ended' " +  // 🔥 ended 상태 제외
            "ORDER BY b.scheduledStartTime ASC")
    List<BroadcastEntity> findBroadcastsByDateRange(
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("nextDay") LocalDateTime nextDay);

    /**
     * 실시간 방송 상태를 고려한 스케줄 조회 (개선된 버전)
     */
    @Query("SELECT b FROM BroadcastEntity b WHERE " +
            "DATE(b.scheduledStartTime) = :date " +
            "ORDER BY b.scheduledStartTime ASC")
    List<BroadcastEntity> findBroadcastScheduleByDateWithStatus(@Param("date") LocalDate date);

    @Query(value = "SELECT * FROM tb_live_broadcasts " +
            "WHERE DATE(scheduled_start_time) = :date " +
            "ORDER BY scheduled_start_time ASC",
            nativeQuery = true)
    List<BroadcastEntity> findBroadcastsByDateNative(@Param("date") String date);

    /**
     * 현재 진행 중인 방송들 조회 (live, paused 상태)
     */
    @Query("SELECT b FROM BroadcastEntity b WHERE " +
            "b.broadcastStatus IN ('live', 'paused') " +
            "ORDER BY b.actualStartTime DESC")
    List<BroadcastEntity> findCurrentActiveBroadcasts();

    // ============ 자동 상태 업데이트 쿼리들 ============

    /**
     * 시작 시간이 지난 scheduled 방송들을 starting으로 변경
     */
    @Modifying
    @Query("UPDATE BroadcastEntity b SET b.broadcastStatus = 'starting', b.updatedAt = CURRENT_TIMESTAMP " +
            "WHERE b.broadcastStatus = 'scheduled' AND b.scheduledStartTime <= CURRENT_TIMESTAMP")
    int updateScheduledToStarting();

    /**
     * 일정 시간이 지난 starting 방송들을 자동으로 ended로 변경
     */
    @Modifying
    @Query("UPDATE BroadcastEntity b SET b.broadcastStatus = 'ended', b.updatedAt = CURRENT_TIMESTAMP " +
            "WHERE b.broadcastStatus = 'starting' AND b.scheduledStartTime < :cutoffTime")
    int updateStartingToEnded(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * 예정 종료 시간이 지난 live 방송들을 자동으로 ended로 변경
     */
    @Modifying
    @Query("UPDATE BroadcastEntity b SET b.broadcastStatus = 'ended', b.actualEndTime = CURRENT_TIMESTAMP, " +
            "b.updatedAt = CURRENT_TIMESTAMP " +
            "WHERE b.broadcastStatus = 'live' AND b.scheduledEndTime IS NOT NULL AND " +
            "b.scheduledEndTime < CURRENT_TIMESTAMP")
    int updateOverdueLiveToEnded();

    /**
     * 관리자용: 특정 방송을 직접 시작으로 변경
     */
    @Modifying
    @Query("UPDATE BroadcastEntity b SET b.broadcastStatus = 'live', b.actualStartTime = CURRENT_TIMESTAMP, " +
            "b.updatedAt = CURRENT_TIMESTAMP " +
            "WHERE b.broadcastId = :broadcastId")
    int updateBroadcastToLive(@Param("broadcastId") Long broadcastId);

    /**
     * 관리자용: 특정 방송을 직접 종료로 변경
     */
    @Modifying
    @Query("UPDATE BroadcastEntity b SET b.broadcastStatus = 'ended', b.actualEndTime = CURRENT_TIMESTAMP, " +
            "b.updatedAt = CURRENT_TIMESTAMP " +
            "WHERE b.broadcastId = :broadcastId")
    int updateBroadcastToEnded(@Param("broadcastId") Long broadcastId);

    /**
     * 관리자용: 특정 방송을 일시정지로 변경
     */
    @Modifying
    @Query("UPDATE BroadcastEntity b SET b.broadcastStatus = 'paused', b.updatedAt = CURRENT_TIMESTAMP " +
            "WHERE b.broadcastId = :broadcastId AND b.broadcastStatus = 'live'")
    int updateBroadcastToPaused(@Param("broadcastId") Long broadcastId);

    /**
     * 관리자용: 일시정지된 방송을 다시 live로 변경
     */
    @Modifying
    @Query("UPDATE BroadcastEntity b SET b.broadcastStatus = 'live', b.updatedAt = CURRENT_TIMESTAMP " +
            "WHERE b.broadcastId = :broadcastId AND b.broadcastStatus = 'paused'")
    int updateBroadcastToResume(@Param("broadcastId") Long broadcastId);

    /**
     * 관리자용: 방송 상태를 직접 변경 (범용)
     */
    @Modifying
    @Query("UPDATE BroadcastEntity b SET b.broadcastStatus = :newStatus, b.updatedAt = CURRENT_TIMESTAMP " +
            "WHERE b.broadcastId = :broadcastId")
    int updateBroadcastStatus(@Param("broadcastId") Long broadcastId, @Param("newStatus") String newStatus);

    /**
     * 관리자용: 여러 방송을 한번에 종료
     */
    @Modifying
    @Query("UPDATE BroadcastEntity b SET b.broadcastStatus = 'ended', b.actualEndTime = CURRENT_TIMESTAMP, " +
            "b.updatedAt = CURRENT_TIMESTAMP " +
            "WHERE b.broadcastId IN :broadcastIds")
    int updateMultipleBroadcastsToEnded(@Param("broadcastIds") List<Long> broadcastIds);

    /**
     * 현재 시간 기준으로 시작해야 할 방송들 조회
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
     * 특정 시간 이후 시작하는 예약된 방송들 조회
     */
    @Query("SELECT b FROM BroadcastEntity b WHERE " +
            "b.scheduledStartTime >= :fromTime " +
            "AND b.broadcastStatus = 'scheduled' " +
            "ORDER BY b.scheduledStartTime ASC")
    List<BroadcastEntity> findUpcomingScheduledBroadcasts(@Param("fromTime") LocalDateTime fromTime);

    /**
     * 특정 날짜의 예약된 방송들 조회
     */
    @Query("SELECT b FROM BroadcastEntity b WHERE " +
            "DATE(b.scheduledStartTime) = :date " +
            "AND b.broadcastStatus = 'scheduled' " +
            "ORDER BY b.scheduledStartTime ASC")
    List<BroadcastEntity> findScheduledBroadcastsByDate(@Param("date") LocalDate date);

    /**
     * 방송자 ID 조회
     */
    @Query("SELECT b.broadcasterId FROM BroadcastEntity b WHERE b.broadcastId = :broadcastId")
    Optional<String> findBroadcasterIdByBroadcastId(@Param("broadcastId") Long broadcastId);

    // ============ 프론트엔드용 핵심 메서드들 ============

    /**
     * 라이브 방송 목록 조회 (전체)
     */
    @Query(value = "SELECT * FROM tb_live_broadcasts " +
            "WHERE broadcast_status = :status AND is_public = :isPublic " +
            "ORDER BY current_viewers DESC, actual_start_time DESC " +
            "LIMIT :limit OFFSET :offset",
            nativeQuery = true)
    List<BroadcastEntity> findLiveBroadcastsForFrontendNative(
            @Param("status") String broadcastStatus,
            @Param("isPublic") Boolean isPublic,
            @Param("limit") int limit,
            @Param("offset") int offset);

    /**
     * 상품 카테고리별 라이브 방송 조회
     */
    @Query(value = "SELECT DISTINCT b.* FROM tb_live_broadcasts b " +
            "LEFT JOIN tb_broadcast_products bp ON b.broadcast_id = bp.broadcast_id " +
            "LEFT JOIN tb_product p ON bp.product_id = p.product_id " +
            "WHERE b.broadcast_status = :status " +
            "AND b.is_public = :isPublic " +
            "AND p.category_id = :categoryId " +
            "ORDER BY b.current_viewers DESC, b.actual_start_time DESC " +
            "LIMIT :limit OFFSET :offset",
            nativeQuery = true)
    List<BroadcastEntity> findLiveBroadcastsByProductCategoryForFrontendNative(
            @Param("status") String broadcastStatus,
            @Param("isPublic") Boolean isPublic,
            @Param("categoryId") Integer categoryId,
            @Param("limit") int limit,
            @Param("offset") int offset);

    /**
     * 부모 카테고리(대분류) 기준 방송 조회
     */
    @Query(value = "SELECT DISTINCT b.* FROM tb_live_broadcasts b " +
            "LEFT JOIN tb_broadcast_products bp ON b.broadcast_id = bp.broadcast_id " +
            "LEFT JOIN tb_product p ON bp.product_id = p.product_id " +
            "LEFT JOIN tb_category c ON p.category_id = c.category_id " +
            "WHERE b.broadcast_status = :status " +
            "AND b.is_public = :isPublic " +
            "AND c.parent_category_id = :parentCategoryId " +
            "ORDER BY b.current_viewers DESC, b.actual_start_time DESC " +
            "LIMIT :limit OFFSET :offset", nativeQuery = true)
    List<BroadcastEntity> findLiveBroadcastsByParentCategoryNative(
            @Param("status") String broadcastStatus,
            @Param("isPublic") Boolean isPublic,
            @Param("parentCategoryId") Integer parentCategoryId,
            @Param("limit") int limit,
            @Param("offset") int offset);

    /**
     * 기존 방송 카테고리 기준 조회 (fallback용)
     */
    @Query(value = "SELECT * FROM tb_live_broadcasts " +
            "WHERE broadcast_status = :status " +
            "AND is_public = :isPublic " +
            "AND category_id = :categoryId " +
            "ORDER BY current_viewers DESC, actual_start_time DESC " +
            "LIMIT :limit OFFSET :offset",
            nativeQuery = true)
    List<BroadcastEntity> findLiveBroadcastsByCategoryForFrontendNative(
            @Param("status") String broadcastStatus,
            @Param("isPublic") Boolean isPublic,
            @Param("categoryId") Integer categoryId,
            @Param("limit") int limit,
            @Param("offset") int offset);

    /**
     * 방송자별 방송 목록 조회 (페이징)
     */
    @Query("SELECT b FROM BroadcastEntity b WHERE b.broadcasterId = :broadcasterId ORDER BY b.createdAt DESC")
    Page<BroadcastEntity> findByBroadcasterIdWithPaging(
            @Param("broadcasterId") String broadcasterId,
            Pageable pageable);

    /**
     * 특정 날짜 범위의 방송 일정 조회 (캘린더용)
     */
    @Query("SELECT b FROM BroadcastEntity b WHERE " +
            "(b.scheduledStartTime BETWEEN :startOfDay AND :endOfDay) " +
            "OR (b.actualStartTime BETWEEN :startOfDay AND :endOfDay) " +
            "ORDER BY COALESCE(b.scheduledStartTime, b.actualStartTime) ASC")
    List<BroadcastEntity> findBroadcastScheduleByDateRange(
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay);

    /**
     * 추천 방송 조회 (인기 + 최신)
     */
    @Query("SELECT b FROM BroadcastEntity b WHERE " +
            "b.broadcastStatus = 'live' AND b.isPublic = true " +
            "AND b.currentViewers > 0 " +
            "ORDER BY (b.currentViewers * 0.7 + b.likeCount * 0.3) DESC")
    List<BroadcastEntity> findRecommendedBroadcasts(Pageable pageable);

    /**
     * 방송 검색 (제목 + 설명)
     */
    @Query("SELECT b FROM BroadcastEntity b WHERE " +
            "(b.title LIKE %:keyword% OR b.description LIKE %:keyword%) " +
            "AND b.isPublic = true " +
            "ORDER BY b.currentViewers DESC")
    List<BroadcastEntity> findByTitleOrDescriptionContaining(@Param("keyword") String keyword, Pageable pageable);

    // ============ 기본 조회 메서드들 (Spring Data JPA 자동 생성) ============

    /**
     * 방송 상태별 개수 조회
     */
    long countByBroadcastStatus(String broadcastStatus);

    /**
     * 방송 상태별 조회
     */
    List<BroadcastEntity> findByBroadcastStatus(String broadcastStatus);

    /**
     * 특정 시간 범위에서 시작하는 방송들 조회 (상태별)
     */
    List<BroadcastEntity> findByScheduledStartTimeBetweenAndBroadcastStatus(
            LocalDateTime startTime,
            LocalDateTime endTime,
            String broadcastStatus
    );

    /**
     * 특정 시간 범위에서 시작하는 모든 방송들 조회
     */
    List<BroadcastEntity> findByScheduledStartTimeBetween(
            LocalDateTime startTime,
            LocalDateTime endTime
    );

    /**
     * 방송 상태별 조회 (정렬)
     */
    List<BroadcastEntity> findByBroadcastStatusOrderByScheduledStartTimeAsc(String broadcastStatus);

    /**
     * 여러 상태의 방송들 조회
     */
    List<BroadcastEntity> findByBroadcastStatusIn(List<String> statuses);

    /**
     * 방송자별 방송 목록 조회
     */
    List<BroadcastEntity> findByBroadcasterIdOrderByScheduledStartTimeDesc(String broadcasterId);

    /**
     * 방송자별 방송 개수 조회
     */
    long countByBroadcasterId(String broadcasterId);

    /**
     * 공개 방송만 조회
     */
    List<BroadcastEntity> findByIsPublicTrueAndBroadcastStatusOrderByScheduledStartTimeAsc(String broadcastStatus);

    /**
     * 총 시청자 수 조회
     */
    @Query("SELECT COALESCE(SUM(b.currentViewers), 0) FROM BroadcastEntity b WHERE b.broadcastStatus = 'live'")
    Long sumCurrentViewers();

    /**
     * 방송자 이름 조회
     */
    @Query("SELECT COALESCE(m.nickname, m.name) FROM MemberEntity m WHERE m.userId = :broadcasterId")
    Optional<String> findBroadcasterNameById(@Param("broadcasterId") String broadcasterId);

    /**
     * 카테고리 이름 조회
     */
    @Query("SELECT c.name FROM CategoryEntity c WHERE c.categoryId = :categoryId")
    Optional<String> findCategoryNameById(@Param("categoryId") Integer categoryId);

    // ============ 디버깅/카테고리 확인용 메서드들 ============

    /**
     * 하위 카테고리 조회 (대분류인지 확인용)
     */
    @Query(value = "SELECT category_id, name FROM tb_category WHERE parent_category_id = :categoryId", nativeQuery = true)
    List<Object[]> findSubCategoriesByParentId(@Param("categoryId") Integer categoryId);

    /**
     * 특정 카테고리의 상품 개수 조회
     */
    @Query(value = "SELECT COUNT(*) FROM tb_product WHERE category_id = :categoryId", nativeQuery = true)
    Integer countProductsByCategory(@Param("categoryId") Integer categoryId);

    /**
     * 특정 카테고리 상품이 포함된 방송 개수 조회
     */
    @Query(value = "SELECT COUNT(DISTINCT b.broadcast_id) FROM tb_live_broadcasts b " +
            "LEFT JOIN tb_broadcast_products bp ON b.broadcast_id = bp.broadcast_id " +
            "LEFT JOIN tb_product p ON bp.product_id = p.product_id " +
            "WHERE p.category_id = :categoryId", nativeQuery = true)
    Integer countBroadcastsByProductCategory(@Param("categoryId") Integer categoryId);

    /**
     * 부모 카테고리 기준 방송 개수 조회
     */
    @Query(value = "SELECT COUNT(DISTINCT b.broadcast_id) FROM tb_live_broadcasts b " +
            "LEFT JOIN tb_broadcast_products bp ON b.broadcast_id = bp.broadcast_id " +
            "LEFT JOIN tb_product p ON bp.product_id = p.product_id " +
            "LEFT JOIN tb_category c ON p.category_id = c.category_id " +
            "WHERE c.parent_category_id = :categoryId", nativeQuery = true)
    Integer countBroadcastsByParentCategory(@Param("categoryId") Integer categoryId);

    /**
     * 방송-상품-카테고리 매핑 샘플 데이터 조회 (디버깅용)
     */
    @Query(value = "SELECT b.broadcast_id, b.title, p.product_id, p.name, " +
            "p.category_id, c.name FROM tb_live_broadcasts b " +
            "LEFT JOIN tb_broadcast_products bp ON b.broadcast_id = bp.broadcast_id " +
            "LEFT JOIN tb_product p ON bp.product_id = p.product_id " +
            "LEFT JOIN tb_category c ON p.category_id = c.category_id " +
            "WHERE p.category_id = :categoryId LIMIT 5", nativeQuery = true)
    List<Object[]> findSampleBroadcastProductMapping(@Param("categoryId") Integer categoryId);

    /**
     * 상품 카테고리별 방송 통계
     */
    @Query("SELECT p.categoryId, c.name, COUNT(DISTINCT b.broadcastId) FROM BroadcastEntity b " +
            "JOIN BroadcastProductEntity bp ON b.broadcastId = bp.broadcastId " +
            "JOIN ProductEntity p ON bp.productId = p.productId " +
            "JOIN CategoryEntity c ON p.categoryId = c.categoryId " +
            "WHERE b.broadcastStatus = 'live' " +
            "GROUP BY p.categoryId, c.name " +
            "ORDER BY COUNT(DISTINCT b.broadcastId) DESC")
    List<Object[]> findBroadcastCountByProductCategory();
}