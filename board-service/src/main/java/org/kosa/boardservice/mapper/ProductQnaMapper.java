package org.kosa.boardservice.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.kosa.boardservice.dto.PageRequestDto;
import org.kosa.boardservice.dto.ProductQnaDto;
import org.kosa.boardservice.dto.ProductQnaAnswerDto;

import java.util.List;
import java.util.Map;

@Mapper
public interface ProductQnaMapper {

    // 🔥 기본 CRUD 메서드들

    // 전체 Q&A 개수 조회 (검색 조건 포함)
    int getTotalCount(@Param("searchValue") String searchValue);

    // 페이징된 Q&A 목록 조회
    List<ProductQnaDto> selectPagedQnas(@Param("dto") PageRequestDto dto);

    // 상품별 Q&A 조회
    List<ProductQnaDto> selectProductQnasByStringId(
            @Param("productId") String productId,
            @Param("startRow") int startRow,
            @Param("size") int size,
            @Param("sortBy") String sortBy
    );

    // 상품별 Q&A 개수 조회
    int getProductQnaCountByStringId(@Param("productId") String productId);

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

    // 🔥 회원 정보 조회
    String selectMemberNameByUserId(@Param("userId") String userId);

    // 🔥 구매 인증 (리뷰와 동일)
    int checkPurchaseVerification(@Param("userId") String userId, @Param("productId") String productId);

    // 🔥 답변 관련 메서드들

    // 특정 Q&A의 답변 목록 조회
    List<ProductQnaAnswerDto> selectAnswersByQnaId(@Param("qnaId") String qnaId);

    // 답변 등록
    int insertAnswer(ProductQnaAnswerDto answerDto);

    // 답변 수정
    int updateAnswer(ProductQnaAnswerDto answerDto);

    // 답변 삭제
    int deleteAnswer(@Param("answerId") String answerId);

    // Q&A 상태 업데이트 (답변 등록 시)
    int updateQnaStatus(@Param("qnaId") String qnaId, @Param("status") String status);

    // 🔥 사용자별 Q&A 조회 (마이페이지용)
    List<ProductQnaDto> selectQnasByUserId(
            @Param("userId") String userId,
            @Param("startRow") int startRow,
            @Param("size") int size
    );

    // 사용자별 Q&A 개수
    int getUserQnaCount(@Param("userId") String userId);
}