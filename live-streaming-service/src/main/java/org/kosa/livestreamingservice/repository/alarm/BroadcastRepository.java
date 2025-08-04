package org.kosa.livestreamingservice.repository.alarm;

import org.kosa.livestreamingservice.entity.BroadcastEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BroadcastRepository extends JpaRepository<BroadcastEntity, Long> {

    // ============ 기본 검증 메서드 ============
    boolean existsByBroadcastIdAndBroadcasterId(Long broadcastId, String broadcasterId);

    // ============ 핵심 알림 서비스용 메서드들 ============

    /**
     * 특정 날짜 범위의 방송 스케줄 조회 (BroadcastService에서 사용)
     */
    @Query("SELECT b FROM BroadcastEntity b WHERE " +
            "b.scheduledStartTime >= :startOfDay AND b.scheduledStartTime < :nextDay " +
            "AND b.broadcastStatus != 'ended' " +
            "ORDER BY b.scheduledStartTime ASC")
    List<BroadcastEntity> findBroadcastsByDateRange(
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("nextDay") LocalDateTime nextDay);

    /**
     * 특정 시간 범위에서 시작하는 방송들 조회 (알림 배치용)
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
     * 방송자 ID 조회 (알림용)
     */
    @Query("SELECT b.broadcasterId FROM BroadcastEntity b WHERE b.broadcastId = :broadcastId")
    Optional<String> findBroadcasterIdByBroadcastId(@Param("broadcastId") Long broadcastId);

    // ============ 프론트엔드용 핵심 메서드들 ============

    /**
     * 라이브 방송 목록 조회 (BroadcastService에서 사용)
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
     * 카테고리별 라이브 방송 조회 (통합된 메서드)
     */
    @Query(value = "SELECT DISTINCT b.* FROM tb_live_broadcasts b " +
            "INNER JOIN tb_broadcast_products bp ON b.broadcast_id = bp.broadcast_id " +
            "INNER JOIN tb_product p ON bp.product_id = p.product_id " +
            "INNER JOIN tb_category c ON p.category_id = c.category_id " +
            "WHERE b.broadcast_status = :status " +
            "AND b.is_public = :isPublic " +
            "AND (c.category_id = :categoryId OR c.parent_category_id = :categoryId) " +
            "ORDER BY b.current_viewers DESC, b.actual_start_time DESC " +
            "LIMIT :limit OFFSET :offset",
            nativeQuery = true)
    List<BroadcastEntity> findLiveBroadcastsByCategoryNative(
            @Param("status") String broadcastStatus,
            @Param("isPublic") Boolean isPublic,
            @Param("categoryId") Integer categoryId,
            @Param("limit") int limit,
            @Param("offset") int offset);

    // ============ BroadcastService에서 사용하는 추가 메서드들 ============

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
     * 추천 방송 조회 (BroadcastService에서 사용)
     */
    @Query("SELECT b FROM BroadcastEntity b WHERE " +
            "b.broadcastStatus = 'live' AND b.isPublic = true " +
            "AND b.currentViewers > 0 " +
            "ORDER BY (b.currentViewers * 0.7 + b.likeCount * 0.3) DESC")
    List<BroadcastEntity> findRecommendedBroadcasts(Pageable pageable);

    /**
     * 방송 검색 (BroadcastService에서 사용)
     */
    @Query("SELECT b FROM BroadcastEntity b WHERE " +
            "(b.title LIKE %:keyword% OR b.description LIKE %:keyword%) " +
            "AND b.isPublic = true " +
            "ORDER BY b.currentViewers DESC")
    List<BroadcastEntity> findByTitleOrDescriptionContaining(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 카테고리 레벨 조회 (BroadcastService에서 사용)
     */
    @Query(value = "SELECT category_level FROM tb_category WHERE category_id = :categoryId",
            nativeQuery = true)
    Optional<Integer> findCategoryLevel(@Param("categoryId") Integer categoryId);

    // ============ 기본 조회 메서드들 ============

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
     * 방송자별 방송 목록 조회
     */
    List<BroadcastEntity> findByBroadcasterIdOrderByScheduledStartTimeDesc(String broadcasterId);

    /**
     * 방송자별 방송 목록 조회 (페이징)
     */
    @Query("SELECT b FROM BroadcastEntity b WHERE b.broadcasterId = :broadcasterId ORDER BY b.createdAt DESC")
    Page<BroadcastEntity> findByBroadcasterIdWithPaging(
            @Param("broadcasterId") String broadcasterId,
            Pageable pageable);

    /**
     * 방송자별 방송 개수 조회
     */
    long countByBroadcasterId(String broadcasterId);

    /**
     * 총 시청자 수 조회
     */
    @Query("SELECT COALESCE(SUM(b.currentViewers), 0) FROM BroadcastEntity b WHERE b.broadcastStatus = 'live'")
    Long sumCurrentViewers();

    /**
     * 방송 상태별 개수 조회
     */
    @Query("SELECT COUNT(b) FROM BroadcastEntity b WHERE b.broadcastStatus = :status")
    Long countByBroadcastStatus(@Param("status") String broadcastStatus);

    /**
     * 방송자 이름 조회 (BroadcastService와 BroadcastViewerService에서 사용)
     */
    @Query("SELECT COALESCE(m.nickname, m.name) FROM MemberEntity m WHERE m.userId = :broadcasterId")
    Optional<String> findBroadcasterNameById(@Param("broadcasterId") String broadcasterId);

    /**
     * 카테고리 이름 조회 (BroadcastService와 BroadcastViewerService에서 사용)
     */
    @Query("SELECT c.name FROM CategoryEntity c WHERE c.categoryId = :categoryId")
    Optional<String> findCategoryNameById(@Param("categoryId") Integer categoryId);

    // ============ 통계용 메서드들 ============

    /**
     * 상품 카테고리별 방송 통계 (BroadcastService에서 사용)
     */
    @Query("SELECT p.categoryId, c.name, COUNT(DISTINCT b.broadcastId) FROM BroadcastEntity b " +
            "JOIN BroadcastProductEntity bp ON b.broadcastId = bp.broadcastId " +
            "JOIN ProductEntity p ON bp.productId = p.productId " +
            "JOIN CategoryEntity c ON p.categoryId = c.categoryId " +
            "WHERE b.broadcastStatus = 'live' " +
            "GROUP BY p.categoryId, c.name " +
            "ORDER BY COUNT(DISTINCT b.broadcastId) DESC")
    List<Object[]> findBroadcastCountByProductCategory();

    /**
     * 카테고리별 방송 개수 조회 (BroadcastService에서 사용)
     */
    @Query(value = "SELECT " +
            "c.category_id, " +
            "c.name, " +
            "c.category_level, " +
            "COUNT(DISTINCT b.broadcast_id) as broadcast_count " +
            "FROM tb_category c " +
            "LEFT JOIN tb_product p ON (c.category_id = p.category_id OR c.category_id = (SELECT parent_category_id FROM tb_category WHERE category_id = p.category_id)) " +
            "LEFT JOIN tb_broadcast_products bp ON p.product_id = bp.product_id " +
            "LEFT JOIN tb_live_broadcasts b ON bp.broadcast_id = b.broadcast_id AND b.broadcast_status = 'live' " +
            "WHERE c.category_use_yn = 'Y' " +
            "GROUP BY c.category_id, c.name, c.category_level " +
            "ORDER BY c.category_level, c.category_display_order",
            nativeQuery = true)
    List<Object[]> findBroadcastCountByAllCategories();
}