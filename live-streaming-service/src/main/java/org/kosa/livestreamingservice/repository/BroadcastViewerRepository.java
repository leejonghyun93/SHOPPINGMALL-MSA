package org.kosa.livestreamingservice.repository;

import org.kosa.livestreamingservice.entity.BroadcastEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BroadcastViewerRepository extends JpaRepository<BroadcastEntity, Long> {

    /**
     * 방송의 상품 목록과 상세 정보 조회
     */
    @Query(value = "SELECT " +
            "p.product_id, " +
            "p.name, " +
            "p.price, " +
            "p.sale_price, " +
            "p.product_description, " +
            "p.main_image, " +
            "p.category_id, " +
            "c.name as category_name, " +
            "bp.display_order, " +
            "bp.special_price, " +
            "bp.is_featured, " +
            "p.stock, " +
            "p.product_rating, " +
            "p.view_count " +
            "FROM tb_broadcast_products bp " +
            "JOIN tb_product p ON bp.product_id = p.product_id " +
            "LEFT JOIN tb_category c ON p.category_id = c.category_id " +
            "WHERE bp.broadcast_id = :broadcastId " +
            "AND p.product_status = 'ACTIVE' " +
            "ORDER BY bp.display_order ASC, bp.created_at ASC",
            nativeQuery = true)
    List<Object[]> findBroadcastProductsWithDetails(@Param("broadcastId") Long broadcastId);

    /**
     * 방송의 메인 상품 조회 (featured 상품)
     */
    @Query(value = "SELECT " +
            "p.product_id, " +
            "p.name, " +
            "p.price, " +
            "p.sale_price, " +
            "p.product_description, " +
            "p.main_image, " +
            "p.category_id, " +
            "c.name as category_name, " +
            "bp.display_order, " +
            "bp.special_price, " +
            "bp.is_featured, " +
            "p.stock, " +
            "p.product_rating, " +
            "p.view_count " +
            "FROM tb_broadcast_products bp " +
            "JOIN tb_product p ON bp.product_id = p.product_id " +
            "LEFT JOIN tb_category c ON p.category_id = c.category_id " +
            "WHERE bp.broadcast_id = :broadcastId " +
            "AND bp.is_featured = true " +
            "AND p.product_status = 'ACTIVE' " +
            "ORDER BY bp.display_order ASC",
            nativeQuery = true)
    List<Object[]> findFeaturedBroadcastProducts(@Param("broadcastId") Long broadcastId);

    /**
     * 방송의 상품 개수 조회
     */
    @Query(value = "SELECT COUNT(*) FROM tb_broadcast_products bp " +
            "JOIN tb_product p ON bp.product_id = p.product_id " +
            "WHERE bp.broadcast_id = :broadcastId " +
            "AND p.product_status = 'ACTIVE'",
            nativeQuery = true)
    Integer countBroadcastProducts(@Param("broadcastId") Long broadcastId);

    /**
     * 방송 시청 기록 저장 (선택사항)
     */
    @Query(value = "INSERT INTO tb_broadcast_views (broadcast_id, viewer_ip, view_time) " +
            "VALUES (:broadcastId, :viewerIp, NOW()) " +
            "ON DUPLICATE KEY UPDATE view_time = NOW()",
            nativeQuery = true)
    void recordBroadcastView(@Param("broadcastId") Long broadcastId, @Param("viewerIp") String viewerIp);

    /**
     * 방송 좋아요 기록 저장 (선택사항)
     */
    @Query(value = "INSERT INTO tb_broadcast_likes (broadcast_id, user_id, like_time) " +
            "VALUES (:broadcastId, :userId, NOW()) " +
            "ON DUPLICATE KEY UPDATE like_time = NOW()",
            nativeQuery = true)
    void recordBroadcastLike(@Param("broadcastId") Long broadcastId, @Param("userId") String userId);

    /**
     * 방송 관련 통계 조회
     */
    @Query(value = "SELECT " +
            "b.broadcast_id, " +
            "b.current_viewers, " +
            "b.peak_viewers, " +
            "b.total_viewers, " +
            "b.like_count, " +
            "COUNT(DISTINCT bp.product_id) as product_count " +
            "FROM tb_live_broadcasts b " +
            "LEFT JOIN tb_broadcast_products bp ON b.broadcast_id = bp.broadcast_id " +
            "WHERE b.broadcast_id = :broadcastId " +
            "GROUP BY b.broadcast_id",
            nativeQuery = true)
    Object[] findBroadcastStats(@Param("broadcastId") Long broadcastId);
}