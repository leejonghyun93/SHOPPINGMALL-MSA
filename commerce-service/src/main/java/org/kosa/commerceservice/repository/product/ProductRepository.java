package org.kosa.commerceservice.repository.product;

import org.kosa.commerceservice.entity.product.Product;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {

    // 특정 상품 ID로 활성 상품 조회 (상품 상세용)
    @Query("SELECT p FROM Product p WHERE p.productId = :productId AND p.productStatus = :productStatus")
    Optional<Product> findByProductIdAndProductStatus(@Param("productId") Integer productId, @Param("productStatus") String productStatus);

    // 연관 상품 조회 (같은 카테고리에서 특정 상품 제외)
    @Query("SELECT p FROM Product p WHERE p.categoryId = :categoryId AND p.productStatus = :productStatus AND p.productId != :excludeProductId ORDER BY p.createdDate DESC")
    List<Product> findByCategoryIdAndProductStatusAndProductIdNot(
            @Param("categoryId") Integer categoryId,
            @Param("productStatus") String productStatus,
            @Param("excludeProductId") Integer excludeProductId,
            Pageable pageable
    );

    // 전체 상품 조회 (카테고리 ALL용) - 이것이 바로 찾던 메서드!
    @Query(value = "SELECT * FROM tb_product WHERE PRODUCT_STATUS = '판매중' AND display_yn = 'Y' ORDER BY CREATED_DATE DESC", nativeQuery = true)
    List<Product> findAllActiveProducts(Pageable pageable);

    // 특정 카테고리 상품 조회 (단일 카테고리)
    @Query(value = "SELECT * FROM tb_product WHERE category_id = :categoryId AND PRODUCT_STATUS = '판매중' AND display_yn = 'Y' ORDER BY CREATED_DATE DESC", nativeQuery = true)
    List<Product> findByCategoryIdActive(@Param("categoryId") Integer categoryId, Pageable pageable);

    // 여러 카테고리 상품 조회 (메인 카테고리 + 하위 카테고리들)
    @Query(value = "SELECT * FROM tb_product WHERE category_id IN (:categoryIds) AND PRODUCT_STATUS = '판매중' AND display_yn = 'Y' ORDER BY CREATED_DATE DESC", nativeQuery = true)
    List<Product> findByMultipleCategoriesActive(@Param("categoryIds") List<Integer> categoryIds, Pageable pageable);

    // HOST별 상품 조회 (라이브 방송용)
    @Query("SELECT p FROM Product p WHERE p.hostId = :hostId AND p.productStatus = :productStatus AND p.displayYn = 'Y' ORDER BY p.createdDate DESC")
    List<Product> findByHostIdAndProductStatus(@Param("hostId") Long hostId, @Param("productStatus") String productStatus, Pageable pageable);

    // 전체 활성 상품 개수 조회 (카테고리 ALL 카운트용)
    @Query(value = "SELECT COUNT(*) FROM tb_product WHERE PRODUCT_STATUS = '판매중' AND display_yn = 'Y'", nativeQuery = true)
    Long countAllActiveProducts();

    // 특정 카테고리 상품 개수 조회
    @Query(value = "SELECT COUNT(*) FROM tb_product WHERE category_id = :categoryId AND PRODUCT_STATUS = '판매중' AND display_yn = 'Y'", nativeQuery = true)
    Long countByCategoryActive(@Param("categoryId") Integer categoryId);

    // 카테고리별 상품 개수 통계 (전체 카테고리)
    @Query(value = "SELECT category_id, COUNT(*) FROM tb_product WHERE PRODUCT_STATUS = '판매중' AND display_yn = 'Y' GROUP BY category_id", nativeQuery = true)
    List<Object[]> getProductCountsByCategory();

    // 여러 상품 ID로 조회 (장바구니, 주문용)
    @Query("SELECT p FROM Product p WHERE p.productId IN :productIds AND p.productStatus = :productStatus AND p.displayYn = 'Y'")
    List<Product> findByProductIdInAndProductStatus(@Param("productIds") List<Integer> productIds, @Param("productStatus") String productStatus);

    // 재고 있는 상품 조회
    @Query("SELECT p FROM Product p WHERE p.stock > 0 AND p.productStatus = :productStatus AND p.displayYn = 'Y' ORDER BY p.createdDate DESC")
    List<Product> findByStockGreaterThanZeroAndProductStatus(@Param("productStatus") String productStatus, Pageable pageable);

    // 할인 상품 조회 (할인율 높은 순)
    @Query("SELECT p FROM Product p WHERE p.salePrice < p.price AND p.productStatus = :productStatus AND p.displayYn = 'Y' ORDER BY ((p.price - p.salePrice) * 100 / p.price) DESC")
    List<Product> findDiscountedProducts(@Param("productStatus") String productStatus, Pageable pageable);

    // 인기 상품 조회 (조회수 높은 순)
    @Query("SELECT p FROM Product p WHERE p.productStatus = :productStatus AND p.displayYn = 'Y' ORDER BY p.viewCount DESC, p.createdDate DESC")
    List<Product> findPopularProducts(@Param("productStatus") String productStatus, Pageable pageable);
}