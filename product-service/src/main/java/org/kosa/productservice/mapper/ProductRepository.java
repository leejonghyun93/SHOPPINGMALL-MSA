package org.kosa.productservice.mapper;

import org.kosa.productservice.entity.Product;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {

    // ================== 메인 상품 조회 메서드들 ==================

    /**
     * 상품 ID와 상태로 단일 상품 조회
     */
    @Query("SELECT p FROM Product p WHERE p.productId = :productId AND p.productStatus = :productStatus")
    Optional<Product> findByProductIdAndProductStatus(@Param("productId") Integer productId, @Param("productStatus") String productStatus);

    /**
     * 카테고리별 상품 조회 (특정 상품 제외) - 연관 상품용
     */
    @Query("SELECT p FROM Product p WHERE p.categoryId = :categoryId AND p.productStatus = :productStatus AND p.productId != :excludeProductId ORDER BY p.createdDate DESC")
    List<Product> findByCategoryIdAndProductStatusAndProductIdNot(
            @Param("categoryId") Integer categoryId,
            @Param("productStatus") String productStatus,
            @Param("excludeProductId") Integer excludeProductId,
            Pageable pageable
    );

    /**
     * 상품 존재 여부 확인
     */
    @Query("SELECT COUNT(p) > 0 FROM Product p WHERE p.productId = :productId AND p.productStatus = :productStatus")
    boolean existsByProductIdAndProductStatus(@Param("productId") Integer productId, @Param("productStatus") String productStatus);

    /**
     * 최신 상품 조회
     */
    @Query("SELECT p FROM Product p WHERE p.productStatus = :productStatus AND p.displayYn = 'Y' ORDER BY p.createdDate DESC")
    List<Product> findByProductStatusOrderByCreatedDateDesc(@Param("productStatus") String productStatus, Pageable pageable);

    // ================== 카테고리별 상품 조회 ==================

    /**
     * 전체 상품 조회 (최신순)
     */
    @Query(value = "SELECT * FROM tb_product WHERE PRODUCT_STATUS = '판매중' AND display_yn = 'Y' " +
            "ORDER BY CREATED_DATE DESC",
            nativeQuery = true)
    List<Product> findAllActiveProducts(Pageable pageable);

    /**
     * 정확한 카테고리 ID로 상품 조회
     */
    @Query(value = "SELECT * FROM tb_product WHERE category_id = :categoryId AND PRODUCT_STATUS = '판매중' AND display_yn = 'Y' " +
            "ORDER BY CREATED_DATE DESC",
            nativeQuery = true)
    List<Product> findByCategoryIdActive(@Param("categoryId") Integer categoryId, Pageable pageable);

    /**
     * 다중 카테고리 상품 조회 - MSA 환경에서 카테고리 서비스로부터 받은 ID 목록으로 조회
     */
    @Query(value = "SELECT * FROM tb_product WHERE category_id IN (:categoryIds) AND PRODUCT_STATUS = '판매중' AND display_yn = 'Y' " +
            "ORDER BY CREATED_DATE DESC",
            nativeQuery = true)
    List<Product> findByMultipleCategoriesActive(@Param("categoryIds") List<Integer> categoryIds, Pageable pageable);

    /**
     * HOST ID로 상품 조회
     */
    @Query("SELECT p FROM Product p WHERE p.hostId = :hostId AND p.productStatus = :productStatus AND p.displayYn = 'Y' ORDER BY p.createdDate DESC")
    List<Product> findByHostIdAndProductStatus(@Param("hostId") Long hostId, @Param("productStatus") String productStatus, Pageable pageable);

    // ================== 카운팅 메서드들 ==================

    /**
     * 전체 활성 상품 개수
     */
    @Query(value = "SELECT COUNT(*) FROM tb_product WHERE PRODUCT_STATUS = '판매중' AND display_yn = 'Y'", nativeQuery = true)
    Long countAllActiveProducts();

    /**
     * 카테고리별 상품 개수
     */
    @Query(value = "SELECT COUNT(*) FROM tb_product WHERE category_id = :categoryId AND PRODUCT_STATUS = '판매중' AND display_yn = 'Y'",
            nativeQuery = true)
    Long countByCategoryActive(@Param("categoryId") Integer categoryId);

    /**
     * 다중 카테고리별 상품 개수
     */
    @Query(value = "SELECT COUNT(*) FROM tb_product WHERE category_id IN (:categoryIds) AND PRODUCT_STATUS = '판매중' AND display_yn = 'Y'",
            nativeQuery = true)
    Long countByMultipleCategoriesActive(@Param("categoryIds") List<Integer> categoryIds);

    /**
     * HOST별 상품 개수
     */
    @Query(value = "SELECT COUNT(*) FROM tb_product WHERE HOST_ID = :hostId AND PRODUCT_STATUS = '판매중' AND display_yn = 'Y'",
            nativeQuery = true)
    Long countByHostIdActive(@Param("hostId") Long hostId);

    /**
     * 카테고리별 상품 개수 통계
     */
    @Query(value = "SELECT category_id, COUNT(*) FROM tb_product WHERE PRODUCT_STATUS = '판매중' AND display_yn = 'Y' GROUP BY category_id",
            nativeQuery = true)
    List<Object[]> getProductCountsByCategory();

    /**
     * 상품에 존재하는 카테고리 ID 목록 조회
     */
    @Query(value = "SELECT DISTINCT category_id FROM tb_product WHERE PRODUCT_STATUS = '판매중' AND display_yn = 'Y' ORDER BY category_id",
            nativeQuery = true)
    List<Integer> findAllActiveCategoryIds();

    /**
     * 상품 ID 목록으로 상품 조회
     */
    @Query("SELECT p FROM Product p WHERE p.productId IN :productIds AND p.productStatus = :productStatus AND p.displayYn = 'Y'")
    List<Product> findByProductIdInAndProductStatus(@Param("productIds") List<Integer> productIds, @Param("productStatus") String productStatus);

    /**
     * 재고가 있는 상품만 조회
     */
    @Query("SELECT p FROM Product p WHERE p.stock > 0 AND p.productStatus = :productStatus AND p.displayYn = 'Y' ORDER BY p.createdDate DESC")
    List<Product> findByStockGreaterThanZeroAndProductStatus(@Param("productStatus") String productStatus, Pageable pageable);

    /**
     * 할인 상품 조회 (판매가 < 정가)
     */
    @Query("SELECT p FROM Product p WHERE p.salePrice < p.price AND p.productStatus = :productStatus AND p.displayYn = 'Y' ORDER BY ((p.price - p.salePrice) * 100 / p.price) DESC")
    List<Product> findDiscountedProducts(@Param("productStatus") String productStatus, Pageable pageable);

    /**
     * 인기 상품 조회 (조회수 기준)
     */
    @Query("SELECT p FROM Product p WHERE p.productStatus = :productStatus AND p.displayYn = 'Y' ORDER BY p.viewCount DESC, p.createdDate DESC")
    List<Product> findPopularProducts(@Param("productStatus") String productStatus, Pageable pageable);
}