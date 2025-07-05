package org.kosa.userservice.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.kosa.userservice.dto.board.PageRequestDto;
import org.kosa.userservice.dto.board.ProductReviewDto;

import java.util.List;
import java.util.Map;

@Mapper
public interface ProductReviewMapper {

    // 전체 게시글 수 조회 (검색 조건 포함)
    int getTotalCount(@Param("searchValue") String searchValue);

    // 페이징된 게시글 목록 조회 (정렬 옵션 포함)
    List<ProductReviewDto> selectPagedBoards(@Param("dto") PageRequestDto dto);

    // 상품별 리뷰 조회 - Integer productId
    List<ProductReviewDto> selectProductReviewsByIntId(
            @Param("productId") Integer productId,
            @Param("startRow") int startRow,
            @Param("size") int size,
            @Param("sortBy") String sortBy
    );

    // 상품별 리뷰 개수 조회 - Integer productId
    int getProductReviewCountByIntId(@Param("productId") Integer productId);

    // 회원 정보 조회
    String selectMemberNameByUserId(@Param("userId") String userId);

    // 구매 인증 - Integer productId
    int checkPurchaseVerification(@Param("userId") String userId, @Param("productId") Integer productId);

    // 구매 상세 정보 조회 - Integer productId
    Map<String, Object> getPurchaseInfo(@Param("userId") String userId, @Param("productId") Integer productId);

    // 리뷰 상세 조회
    ProductReviewDto selectReviewById(@Param("reviewId") String reviewId);

    // 리뷰 등록
    int insertReview(ProductReviewDto reviewDto);

    // 리뷰 수정
    int updateReview(ProductReviewDto reviewDto);

    // 리뷰 삭제
    int deleteReview(@Param("reviewId") String reviewId);

    // 기존 String 기반 메서드들 (호환성 유지용 - 필요에 따라 제거)
    @Deprecated
    List<ProductReviewDto> selectProductReviewsByStringId(
            @Param("productId") String productId,
            @Param("startRow") int startRow,
            @Param("size") int size,
            @Param("sortBy") String sortBy
    );

    @Deprecated
    int getProductReviewCountByStringId(@Param("productId") String productId);
}