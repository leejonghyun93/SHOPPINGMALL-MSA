package org.kosa.commerceservice.repository.product;


import org.kosa.commerceservice.entity.product.Wish;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface WishRepository extends JpaRepository<Wish, String> {

    // 특정 사용자의 특정 상품 찜하기 확인
    Optional<Wish> findByUserIdAndProductId(String userId, Integer productId);

    // 사용자의 모든 찜한 상품 조회
    List<Wish> findByUserIdOrderByCreatedDateDesc(String userId);

    // 사용자의 찜한 상품 개수
    Long countByUserId(String userId);

    // 특정 상품의 찜 개수
    Long countByProductId(Integer productId);

    // 사용자의 찜한 상품을 상품 정보와 함께 조회
    @Query(value = """
        SELECT w.WISH_ID, w.USER_ID, w.PRODUCT_ID, w.CREATED_DATE,
               p.NAME as product_name, p.PRICE as product_price, 
               p.SALE_PRICE as sale_price, p.MAIN_IMAGE as main_image,
               p.PRODUCT_STATUS as product_status
        FROM tb_wish w 
        INNER JOIN tb_product p ON w.PRODUCT_ID = p.PRODUCT_ID 
        WHERE w.USER_ID = :userId AND p.PRODUCT_STATUS = '판매중' AND p.display_yn = 'Y'
        ORDER BY w.CREATED_DATE DESC
        """, nativeQuery = true)
    List<Object[]> findWishListWithProductInfo(@Param("userId") String userId);

    // 사용자와 상품 ID로 찜하기 삭제
    void deleteByUserIdAndProductId(String userId, Integer productId);

    // 사용자 ID로 모든 찜하기 삭제
    void deleteByUserId(String userId);
}