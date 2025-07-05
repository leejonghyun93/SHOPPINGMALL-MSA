package org.kosa.userservice.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.kosa.userservice.dto.board.PageRequestDto;
import org.kosa.userservice.dto.board.ProductQnaDto;
import org.kosa.userservice.dto.board.ProductQnaAnswerDto;

import java.util.List;

@Mapper
public interface ProductQnaMapper {

    // 전체 Q&A 개수 조회 (검색 조건 포함)
    int getTotalCount(@Param("searchValue") String searchValue);

    // 페이징된 Q&A 목록 조회
    List<ProductQnaDto> selectPagedQnas(@Param("dto") PageRequestDto dto);

    // 상품별 Q&A 조회 - Integer productId
    List<ProductQnaDto> selectProductQnasByIntId(
            @Param("productId") Integer productId,
            @Param("startRow") int startRow,
            @Param("size") int size,
            @Param("sortBy") String sortBy
    );

    // 상품별 Q&A 개수 조회 - Integer productId
    int getProductQnaCountByIntId(@Param("productId") Integer productId);

    // Q&A 상세 조회 (답변 포함)
    ProductQnaDto selectQnaById(@Param("qnaId") String qnaId);

    // Q&A 등록
    int insertQna(ProductQnaDto qnaDto);

    // Q&A 수정
    int updateQna(ProductQnaDto qnaDto);

    // Q&A 삭제
    int deleteQna(@Param("qnaId") String qnaId);

    // 조회수 증가
    int increaseViewCount(@Param("qnaId") String qnaId);

    // 회원 정보 조회
    String selectMemberNameByUserId(@Param("userId") String userId);

    // 구매 인증 - Integer productId
    int checkPurchaseVerification(@Param("userId") String userId, @Param("productId") Integer productId);

    // 답변 관련 메서드들
    List<ProductQnaAnswerDto> selectAnswersByQnaId(@Param("qnaId") String qnaId);
    int insertAnswer(ProductQnaAnswerDto answerDto);
    int updateAnswer(ProductQnaAnswerDto answerDto);
    int deleteAnswer(@Param("answerId") String answerId);
    int updateQnaStatus(@Param("qnaId") String qnaId, @Param("status") String status);

    // 사용자별 Q&A 조회 (마이페이지용)
    List<ProductQnaDto> selectQnasByUserId(
            @Param("userId") String userId,
            @Param("startRow") int startRow,
            @Param("size") int size
    );

    // 사용자별 Q&A 개수
    int getUserQnaCount(@Param("userId") String userId);

    // 기존 String 기반 메서드들 (호환성 유지용 - 필요에 따라 제거)
    @Deprecated
    List<ProductQnaDto> selectProductQnasByStringId(
            @Param("productId") String productId,
            @Param("startRow") int startRow,
            @Param("size") int size,
            @Param("sortBy") String sortBy
    );

    @Deprecated
    int getProductQnaCountByStringId(@Param("productId") String productId);
}