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
public interface ProductRepository extends JpaRepository<Product, String> {

    // ================== 메인 상품 조회 메서드들 ==================

    /**
     * 상품 ID와 상태로 단일 상품 조회
     */
    @Query("SELECT p FROM Product p WHERE p.productId = :productId AND p.productStatus = :productStatus")
    Optional<Product> findByProductIdAndProductStatus(@Param("productId") String productId, @Param("productStatus") String productStatus);

    /**
     * 카테고리별 상품 조회 (특정 상품 제외) - 연관 상품용
     */
    @Query("SELECT p FROM Product p WHERE p.categoryId = :categoryId AND p.productStatus = :productStatus AND p.productId != :excludeProductId ORDER BY p.createdDate DESC")
    List<Product> findByCategoryIdAndProductStatusAndProductIdNot(
            @Param("categoryId") String categoryId,
            @Param("productStatus") String productStatus,
            @Param("excludeProductId") String excludeProductId,
            Pageable pageable
    );

    /**
     * 상품 존재 여부 확인
     */
    @Query("SELECT COUNT(p) > 0 FROM Product p WHERE p.productId = :productId AND p.productStatus = :productStatus")
    boolean existsByProductIdAndProductStatus(@Param("productId") String productId, @Param("productStatus") String productStatus);

    /**
     * 최신 상품 조회
     */
    @Query("SELECT p FROM Product p WHERE p.productStatus = :productStatus ORDER BY p.createdDate DESC")
    List<Product> findByProductStatusOrderByCreatedDateDesc(@Param("productStatus") String productStatus, Pageable pageable);

    // ================== 카테고리별 상품 조회 ==================

    /**
     * 전체 상품 조회 (최신순)
     */
    @Query(value = "SELECT * FROM tb_product WHERE PRODUCT_STATUS = 'ACTIVE' " +
            "ORDER BY CREATED_DATE DESC",
            nativeQuery = true)
    List<Product> findAllActiveProducts(Pageable pageable);

    /**
     * 정확한 카테고리 ID로 상품 조회
     */
    @Query(value = "SELECT * FROM tb_product WHERE CATEGORY_ID = :categoryId AND PRODUCT_STATUS = 'ACTIVE' " +
            "ORDER BY CREATED_DATE DESC",
            nativeQuery = true)
    List<Product> findByCategoryIdActive(@Param("categoryId") String categoryId, Pageable pageable);

    /**
     * 메인 카테고리로 하위 카테고리 포함 조회
     */
    @Query(value = "SELECT * FROM tb_product WHERE " +
            "CATEGORY_ID LIKE CONCAT(:parentCategoryId, '%') AND " +
            "PRODUCT_STATUS = 'ACTIVE' " +
            "ORDER BY CREATED_DATE DESC",
            nativeQuery = true)
    List<Product> findByParentCategoryActive(@Param("parentCategoryId") String parentCategoryId, Pageable pageable);

    /**
     * 다중 카테고리 상품 조회 - MSA 환경에서 카테고리 서비스로부터 받은 ID 목록으로 조회
     */
    @Query(value = "SELECT * FROM tb_product WHERE CATEGORY_ID IN (:categoryIds) AND PRODUCT_STATUS = 'ACTIVE' " +
            "ORDER BY CREATED_DATE DESC",
            nativeQuery = true)
    List<Product> findByMultipleCategoriesActive(@Param("categoryIds") List<String> categoryIds, Pageable pageable);

    // ================== 카운팅 메서드들 ==================

    /**
     * 전체 활성 상품 개수
     */
    @Query(value = "SELECT COUNT(*) FROM tb_product WHERE PRODUCT_STATUS = 'ACTIVE'", nativeQuery = true)
    Long countAllActiveProducts();

    /**
     * 카테고리별 상품 개수
     */
    @Query(value = "SELECT COUNT(*) FROM tb_product WHERE CATEGORY_ID = :categoryId AND PRODUCT_STATUS = 'ACTIVE'",
            nativeQuery = true)
    Long countByCategoryActive(@Param("categoryId") String categoryId);

    /**
     * 메인 카테고리의 하위 카테고리 포함 개수 조회
     */
    @Query(value = "SELECT COUNT(*) FROM tb_product WHERE " +
            "CATEGORY_ID LIKE CONCAT(:parentCategoryId, '%') AND " +
            "PRODUCT_STATUS = 'ACTIVE'",
            nativeQuery = true)
    Long countByParentCategoryActive(@Param("parentCategoryId") String parentCategoryId);

    /**
     * 다중 카테고리별 상품 개수
     */
    @Query(value = "SELECT COUNT(*) FROM tb_product WHERE CATEGORY_ID IN (:categoryIds) AND PRODUCT_STATUS = 'ACTIVE'",
            nativeQuery = true)
    Long countByMultipleCategoriesActive(@Param("categoryIds") List<String> categoryIds);

    /**
     * 카테고리별 상품 개수 통계
     */
    @Query(value = "SELECT CATEGORY_ID, COUNT(*) FROM tb_product WHERE PRODUCT_STATUS = 'ACTIVE' GROUP BY CATEGORY_ID",
            nativeQuery = true)
    List<Object[]> getProductCountsByCategory();

    /**
     * 상품에 존재하는 카테고리 ID 목록 조회
     */
    @Query(value = "SELECT DISTINCT CATEGORY_ID FROM tb_product WHERE PRODUCT_STATUS = 'ACTIVE' ORDER BY CATEGORY_ID",
            nativeQuery = true)
    List<String> findAllActiveCategoryIds();

    @Query("SELECT p FROM Product p WHERE p.productId IN :productIds AND p.productStatus = :productStatus")
    List<Product> findByProductIdInAndProductStatus(@Param("productIds") List<String> productIds, @Param("productStatus") String productStatus);

}