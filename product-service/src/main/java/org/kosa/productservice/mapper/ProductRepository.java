package org.kosa.productservice.mapper;

import org.kosa.productservice.dto.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, String> {

    // 카테고리별 상품 조회
    List<Product> findByCategoryIdAndProductStatusOrderByCreatedDateDesc(String categoryId, String status);

    // 활성 상품 조회 (최신순)
    List<Product> findByProductStatusOrderByCreatedDateDesc(String status);

    // 인기 상품 조회 (판매량 기준)
    List<Product> findByProductStatusOrderByProductSalesCountDesc(String status, Pageable pageable);

    // 라이브 상품 조회 (시작일/종료일 기준)
    @Query("SELECT p FROM Product p WHERE p.productStatus = :status " +
            "AND p.startDate <= :now AND (p.endDate IS NULL OR p.endDate >= :now) " +
            "ORDER BY p.productSalesCount DESC")
    List<Product> findLiveProducts(@Param("status") String status, @Param("now") LocalDateTime now, Pageable pageable);

    // 카테고리별 라이브 상품 조회
    @Query("SELECT p FROM Product p WHERE p.categoryId = :categoryId AND p.productStatus = :status " +
            "AND p.startDate <= :now AND (p.endDate IS NULL OR p.endDate >= :now) " +
            "ORDER BY p.productSalesCount DESC")
    List<Product> findLiveProductsByCategory(@Param("categoryId") String categoryId, @Param("status") String status,
                                             @Param("now") LocalDateTime now, Pageable pageable);

    // 검색
    @Query("SELECT p FROM Product p WHERE p.productStatus = :status " +
            "AND (p.name LIKE %:keyword% OR p.productShortDescription LIKE %:keyword%) " +
            "ORDER BY p.createdDate DESC")
    List<Product> searchProducts(@Param("keyword") String keyword, @Param("status") String status);

    // 카테고리 및 하위 카테고리 상품 조회
    @Query("SELECT p FROM Product p WHERE p.categoryId IN :categoryIds AND p.productStatus = :status " +
            "ORDER BY p.createdDate DESC")
    List<Product> findByCategoryIdsAndProductStatus(@Param("categoryIds") List<String> categoryIds, @Param("status") String status);
}
