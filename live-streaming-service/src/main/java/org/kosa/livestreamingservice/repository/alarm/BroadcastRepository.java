package org.kosa.livestreamingservice.repository.alarm;

import org.kosa.livestreamingservice.entity.BroadcastEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
// ============ 디버깅용 메소드들 ============

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
     * 방송-상품-카테고리 매핑 샘플 데이터 조회
     */
    @Query(value = "SELECT b.broadcast_id, b.title, p.product_id, p.name, " +
            "p.category_id, c.name FROM tb_live_broadcasts b " +
            "LEFT JOIN tb_broadcast_products bp ON b.broadcast_id = bp.broadcast_id " +
            "LEFT JOIN tb_product p ON bp.product_id = p.product_id " +
            "LEFT JOIN tb_category c ON p.category_id = c.category_id " +
            "WHERE p.category_id = :categoryId LIMIT 5", nativeQuery = true)
    List<Object[]> findSampleBroadcastProductMapping(@Param("categoryId") Integer categoryId);

    /**
     * 하위 카테고리 조회
     */
    @Query(value = "SELECT category_id, name FROM tb_category WHERE parent_category_id = :categoryId", nativeQuery = true)
    List<Object[]> findSubCategoriesByParentId(@Param("categoryId") Integer categoryId);

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
     * 부모 카테고리(대분류) 기준 방송 조회 - 하위 카테고리 상품들 포함
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
     * 모든 방송-상품-카테고리 매핑 상태 확인
     */
    @Query(value = "SELECT b.broadcast_id, b.title, b.broadcast_status, " +
            "p.product_id, p.name as product_name, " +
            "c.category_id, c.name as category_name, c.parent_category_id " +
            "FROM tb_live_broadcasts b " +
            "LEFT JOIN tb_broadcast_products bp ON b.broadcast_id = bp.broadcast_id " +
            "LEFT JOIN tb_product p ON bp.product_id = p.product_id " +
            "LEFT JOIN tb_category c ON p.category_id = c.category_id " +
            "WHERE b.broadcast_status = 'live' " +
            "ORDER BY b.broadcast_id, p.product_id LIMIT 20", nativeQuery = true)
    List<Object[]> findAllBroadcastProductMappings();

    /**
     * 카테고리별 구조 확인
     */
    @Query(value = "SELECT c.category_id, c.name, c.parent_category_id, c.category_level, " +
            "(SELECT COUNT(*) FROM tb_product p WHERE p.category_id = c.category_id) as product_count " +
            "FROM tb_category c " +
            "ORDER BY c.category_level, c.parent_category_id, c.category_display_order", nativeQuery = true)
    List<Object[]> findCategoryStructure();
    // ============ 기존 알림 서비스용 메소드들 ============

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

    // ========== broadcaster_id 관련 메서드들 ==========

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

    // ============ 프론트엔드용 메소드들 ============

    /**
     * 라이브 방송 목록 조회 (프론트엔드용) - 전체 방송 (Native Query)
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
     * 라이브 방송 목록 조회 (프론트엔드용) - 전체 방송 (JPQL)
     */
    @Query("SELECT b FROM BroadcastEntity b WHERE " +
            "b.broadcastStatus = :status AND b.isPublic = :isPublic " +
            "ORDER BY b.currentViewers DESC, b.actualStartTime DESC")
    List<BroadcastEntity> findLiveBroadcastsForFrontend(
            @Param("status") String broadcastStatus,
            @Param("isPublic") Boolean isPublic,
            Pageable pageable);

    /**
     * 상품 카테고리별 라이브 방송 조회 (Native Query) - LEFT JOIN을 통해 상품의 카테고리로 필터링
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
     * 상품 카테고리별 라이브 방송 조회 (JPQL) - JOIN을 통해 상품의 카테고리로 필터링
     */
    @Query("SELECT DISTINCT b FROM BroadcastEntity b " +
            "JOIN BroadcastProductEntity bp ON b.broadcastId = bp.broadcastId " +
            "JOIN ProductEntity p ON bp.productId = p.productId " +
            "WHERE b.broadcastStatus = :status AND b.isPublic = :isPublic " +
            "AND p.categoryId = :categoryId " +
            "ORDER BY b.currentViewers DESC, b.actualStartTime DESC")
    List<BroadcastEntity> findLiveBroadcastsByProductCategoryForFrontend(
            @Param("status") String broadcastStatus,
            @Param("isPublic") Boolean isPublic,
            @Param("categoryId") Integer categoryId,
            Pageable pageable);

    /**
     * 기존 방송 카테고리 기준 조회 (Native Query)
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
     * 기존 방송 카테고리 기준 조회 (JPQL - 호환성 유지)
     */
    @Query("SELECT b FROM BroadcastEntity b WHERE " +
            "b.broadcastStatus = :status AND b.isPublic = :isPublic AND b.categoryId = :categoryId " +
            "ORDER BY b.currentViewers DESC, b.actualStartTime DESC")
    List<BroadcastEntity> findLiveBroadcastsByCategoryForFrontend(
            @Param("status") String broadcastStatus,
            @Param("isPublic") Boolean isPublic,
            @Param("categoryId") Integer categoryId,
            Pageable pageable);

    /**
     * 방송자별 방송 목록 조회 (페이징)
     */
    @Query("SELECT b FROM BroadcastEntity b WHERE b.broadcasterId = :broadcasterId ORDER BY b.createdAt DESC")
    Page<BroadcastEntity> findByBroadcasterIdWithPaging(
            @Param("broadcasterId") String broadcasterId,
            Pageable pageable);

    /**
     * 특정 날짜 범위의 방송 일정 조회 (프론트엔드 캘린더용)
     */
    @Query("SELECT b FROM BroadcastEntity b WHERE " +
            "(b.scheduledStartTime BETWEEN :startOfDay AND :endOfDay) " +
            "OR (b.actualStartTime BETWEEN :startOfDay AND :endOfDay) " +
            "ORDER BY COALESCE(b.scheduledStartTime, b.actualStartTime) ASC")
    List<BroadcastEntity> findBroadcastScheduleByDateRange(
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay);

    /**
     * 활성 방송 목록 조회 (진행중, 시작중, 일시정지 상태)
     */
    @Query("SELECT b FROM BroadcastEntity b WHERE " +
            "b.broadcastStatus IN ('live', 'starting', 'paused') " +
            "AND b.isPublic = true " +
            "ORDER BY b.currentViewers DESC")
    List<BroadcastEntity> findActiveBroadcasts(Pageable pageable);

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

    // ============ 통계 관련 메소드들 ============

    /**
     * 방송 통계 - 총 시청자 수
     */
    @Query("SELECT COALESCE(SUM(b.currentViewers), 0) FROM BroadcastEntity b WHERE b.broadcastStatus = 'live'")
    Long sumCurrentViewers();

    /**
     * 방송 통계 - 방송 상태별 개수
     */
    long countByBroadcastStatus(String broadcastStatus);

    /**
     * 방송 통계 - 카테고리별 방송 개수 (방송 자체 카테고리 기준)
     */
    @Query("SELECT b.categoryId, COUNT(b) FROM BroadcastEntity b " +
            "WHERE b.broadcastStatus = 'live' " +
            "GROUP BY b.categoryId " +
            "ORDER BY COUNT(b) DESC")
    List<Object[]> findBroadcastCountByCategory();

    /**
     * 방송 통계 - 상품 카테고리별 방송 개수 (상품 카테고리 기준)
     */
    @Query("SELECT p.categoryId, c.name, COUNT(DISTINCT b.broadcastId) FROM BroadcastEntity b " +
            "JOIN BroadcastProductEntity bp ON b.broadcastId = bp.broadcastId " +
            "JOIN ProductEntity p ON bp.productId = p.productId " +
            "JOIN CategoryEntity c ON p.categoryId = c.categoryId " +
            "WHERE b.broadcastStatus = 'live' " +
            "GROUP BY p.categoryId, c.name " +
            "ORDER BY COUNT(DISTINCT b.broadcastId) DESC")
    List<Object[]> findBroadcastCountByProductCategory();

    // ============ 외부 데이터 조회 메소드들 ============

    /**
     * 카테고리 ID로 카테고리명 조회
     */
    @Query("SELECT c.name FROM CategoryEntity c WHERE c.categoryId = :categoryId")
    Optional<String> findCategoryNameById(@Param("categoryId") Integer categoryId);

    /**
     * 방송자 ID로 방송자명 조회 (닉네임 우선, 없으면 이름)
     */
    @Query("SELECT COALESCE(m.nickname, m.name) FROM MemberEntity m WHERE m.userId = :broadcasterId")
    Optional<String> findBroadcasterNameById(@Param("broadcasterId") String broadcasterId);

    /**
     * 상품 카테고리 기반 방송 조회 시 카테고리명도 함께 조회 (Native Query)
     */
    @Query(value = "SELECT DISTINCT b.*, c.name as category_name FROM tb_live_broadcasts b " +
            "LEFT JOIN tb_broadcast_products bp ON b.broadcast_id = bp.broadcast_id " +
            "LEFT JOIN tb_product p ON bp.product_id = p.product_id " +
            "LEFT JOIN tb_category c ON p.category_id = c.category_id " +
            "WHERE b.broadcast_status = :status " +
            "AND b.is_public = :isPublic " +
            "AND p.category_id = :categoryId " +
            "ORDER BY b.current_viewers DESC, b.actual_start_time DESC " +
            "LIMIT :limit OFFSET :offset",
            nativeQuery = true)
    List<Object[]> findLiveBroadcastsWithCategoryByProductCategoryNative(
            @Param("status") String broadcastStatus,
            @Param("isPublic") Boolean isPublic,
            @Param("categoryId") Integer categoryId,
            @Param("limit") int limit,
            @Param("offset") int offset);

    /**
     * 상품 카테고리 기반 방송 조회 시 카테고리명도 함께 조회 (JPQL)
     */
    @Query("SELECT DISTINCT b, c.name FROM BroadcastEntity b " +
            "JOIN BroadcastProductEntity bp ON b.broadcastId = bp.broadcastId " +
            "JOIN ProductEntity p ON bp.productId = p.productId " +
            "JOIN CategoryEntity c ON p.categoryId = c.categoryId " +
            "WHERE b.broadcastStatus = :status AND b.isPublic = :isPublic " +
            "AND p.categoryId = :categoryId " +
            "ORDER BY b.currentViewers DESC, b.actualStartTime DESC")
    List<Object[]> findLiveBroadcastsWithCategoryByProductCategory(
            @Param("status") String broadcastStatus,
            @Param("isPublic") Boolean isPublic,
            @Param("categoryId") Integer categoryId,
            Pageable pageable);

    /**
     * 하위 카테고리를 포함한 상품 카테고리 기반 방송 조회 (Native Query)
     */
    @Query(value = "SELECT DISTINCT b.*, c.name as category_name FROM tb_live_broadcasts b " +
            "LEFT JOIN tb_broadcast_products bp ON b.broadcast_id = bp.broadcast_id " +
            "LEFT JOIN tb_product p ON bp.product_id = p.product_id " +
            "LEFT JOIN tb_category c ON p.category_id = c.category_id " +
            "WHERE b.broadcast_status = :status " +
            "AND b.is_public = :isPublic " +
            "AND (p.category_id = :categoryId OR c.parent_category_id = :categoryId) " +
            "ORDER BY b.current_viewers DESC, b.actual_start_time DESC " +
            "LIMIT :limit OFFSET :offset",
            nativeQuery = true)
    List<Object[]> findLiveBroadcastsWithCategoryByProductCategoryIncludingSubCategoriesNative(
            @Param("status") String broadcastStatus,
            @Param("isPublic") Boolean isPublic,
            @Param("categoryId") Integer categoryId,
            @Param("limit") int limit,
            @Param("offset") int offset);

    /**
     * 하위 카테고리를 포함한 상품 카테고리 기반 방송 조회 (JPQL)
     */
    @Query("SELECT DISTINCT b, c.name FROM BroadcastEntity b " +
            "JOIN BroadcastProductEntity bp ON b.broadcastId = bp.broadcastId " +
            "JOIN ProductEntity p ON bp.productId = p.productId " +
            "JOIN CategoryEntity c ON p.categoryId = c.categoryId " +
            "WHERE b.broadcastStatus = :status AND b.isPublic = :isPublic " +
            "AND (p.categoryId = :categoryId OR c.parentCategoryId = :categoryId) " +
            "ORDER BY b.currentViewers DESC, b.actualStartTime DESC")
    List<Object[]> findLiveBroadcastsWithCategoryByProductCategoryIncludingSubCategories(
            @Param("status") String broadcastStatus,
            @Param("isPublic") Boolean isPublic,
            @Param("categoryId") Integer categoryId,
            Pageable pageable);

    /**
     * 특정 방송의 연관 상품들과 카테고리 정보 조회
     */
    @Query("SELECT p, c.name FROM BroadcastProductEntity bp " +
            "JOIN ProductEntity p ON bp.productId = p.productId " +
            "JOIN CategoryEntity c ON p.categoryId = c.categoryId " +
            "WHERE bp.broadcastId = :broadcastId " +
            "ORDER BY bp.displayOrder ASC")
    List<Object[]> findBroadcastProductsWithCategory(@Param("broadcastId") Long broadcastId);

    /**
     * 방송에 등록된 상품의 카테고리들 조회
     */
    @Query("SELECT DISTINCT c.categoryId, c.name FROM BroadcastProductEntity bp " +
            "JOIN ProductEntity p ON bp.productId = p.productId " +
            "JOIN CategoryEntity c ON p.categoryId = c.categoryId " +
            "WHERE bp.broadcastId = :broadcastId")
    List<Object[]> findBroadcastCategories(@Param("broadcastId") Long broadcastId);
}