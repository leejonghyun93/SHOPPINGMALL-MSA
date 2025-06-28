package org.kosa.boardservice.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.kosa.boardservice.dto.PageRequestDto;
import org.kosa.boardservice.dto.ProductReviewDto;

import java.util.List;
import java.util.Map;

@Mapper
public interface ProductReviewMapper {

    // 전체 게시글 수 조회 (검색 조건 포함)
    int getTotalCount(@Param("searchValue") String searchValue);

    // 페이징된 게시글 목록 조회 (정렬 옵션 포함)
    List<ProductReviewDto> selectPagedBoards(@Param("dto") PageRequestDto dto);

    // 상품별 리뷰 조회 - startRow로 변경
    List<ProductReviewDto> selectProductReviewsByStringId(
            @Param("productId") String productId,
            @Param("startRow") int startRow,
            @Param("size") int size,
            @Param("sortBy") String sortBy
    );
    String selectMemberNameByUserId(@Param("userId") String userId);

    // 구매 인증: 해당 사용자가 해당 상품을 구매했고 배송완료인지 확인
    int checkPurchaseVerification(@Param("userId") String userId, @Param("productId") String productId);

    //  구매 상세 정보 조회 (선택사항)
    Map<String, Object> getPurchaseInfo(@Param("userId") String userId, @Param("productId") String productId);
    // 상품별 리뷰 개수 조회 - String
    int getProductReviewCountByStringId(@Param("productId") String productId);

    //  리뷰 상세 조회
    ProductReviewDto selectReviewById(@Param("reviewId") String reviewId);

    //  리뷰 등록
    int insertReview(ProductReviewDto reviewDto);

    //  리뷰 수정
    int updateReview(ProductReviewDto reviewDto);

    //  리뷰 삭제
    int deleteReview(@Param("reviewId") String reviewId);
}