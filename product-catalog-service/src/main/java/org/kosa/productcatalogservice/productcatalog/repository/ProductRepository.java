package org.kosa.productcatalogservice.productcatalog.repository;


import org.kosa.productcatalogservice.productcatalog.entity.Product;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {

    @Query("SELECT p FROM Product p WHERE p.productId = :productId AND p.productStatus = :productStatus")
    Optional<Product> findByProductIdAndProductStatus(@Param("productId") Integer productId, @Param("productStatus") String productStatus);

    @Query("SELECT p FROM Product p WHERE p.categoryId = :categoryId AND p.productStatus = :productStatus AND p.productId != :excludeProductId ORDER BY p.createdDate DESC")
    List<Product> findByCategoryIdAndProductStatusAndProductIdNot(
            @Param("categoryId") Integer categoryId,
            @Param("productStatus") String productStatus,
            @Param("excludeProductId") Integer excludeProductId,
            Pageable pageable
    );

    @Query(value = "SELECT * FROM tb_product WHERE PRODUCT_STATUS = '판매중' AND display_yn = 'Y' ORDER BY CREATED_DATE DESC", nativeQuery = true)
    List<Product> findAllActiveProducts(Pageable pageable);

    @Query(value = "SELECT * FROM tb_product WHERE category_id = :categoryId AND PRODUCT_STATUS = '판매중' AND display_yn = 'Y' ORDER BY CREATED_DATE DESC", nativeQuery = true)
    List<Product> findByCategoryIdActive(@Param("categoryId") Integer categoryId, Pageable pageable);

    @Query(value = "SELECT * FROM tb_product WHERE category_id IN (:categoryIds) AND PRODUCT_STATUS = '판매중' AND display_yn = 'Y' ORDER BY CREATED_DATE DESC", nativeQuery = true)
    List<Product> findByMultipleCategoriesActive(@Param("categoryIds") List<Integer> categoryIds, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.hostId = :hostId AND p.productStatus = :productStatus AND p.displayYn = 'Y' ORDER BY p.createdDate DESC")
    List<Product> findByHostIdAndProductStatus(@Param("hostId") Long hostId, @Param("productStatus") String productStatus, Pageable pageable);

    @Query(value = "SELECT COUNT(*) FROM tb_product WHERE PRODUCT_STATUS = '판매중' AND display_yn = 'Y'", nativeQuery = true)
    Long countAllActiveProducts();

    @Query(value = "SELECT COUNT(*) FROM tb_product WHERE category_id = :categoryId AND PRODUCT_STATUS = '판매중' AND display_yn = 'Y'", nativeQuery = true)
    Long countByCategoryActive(@Param("categoryId") Integer categoryId);

    @Query(value = "SELECT category_id, COUNT(*) FROM tb_product WHERE PRODUCT_STATUS = '판매중' AND display_yn = 'Y' GROUP BY category_id", nativeQuery = true)
    List<Object[]> getProductCountsByCategory();

    @Query("SELECT p FROM Product p WHERE p.productId IN :productIds AND p.productStatus = :productStatus AND p.displayYn = 'Y'")
    List<Product> findByProductIdInAndProductStatus(@Param("productIds") List<Integer> productIds, @Param("productStatus") String productStatus);

    @Query("SELECT p FROM Product p WHERE p.stock > 0 AND p.productStatus = :productStatus AND p.displayYn = 'Y' ORDER BY p.createdDate DESC")
    List<Product> findByStockGreaterThanZeroAndProductStatus(@Param("productStatus") String productStatus, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.salePrice < p.price AND p.productStatus = :productStatus AND p.displayYn = 'Y' ORDER BY ((p.price - p.salePrice) * 100 / p.price) DESC")
    List<Product> findDiscountedProducts(@Param("productStatus") String productStatus, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.productStatus = :productStatus AND p.displayYn = 'Y' ORDER BY p.viewCount DESC, p.createdDate DESC")
    List<Product> findPopularProducts(@Param("productStatus") String productStatus, Pageable pageable);
}
