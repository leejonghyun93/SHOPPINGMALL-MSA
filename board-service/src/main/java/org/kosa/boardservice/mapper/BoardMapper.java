package org.kosa.boardservice.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.kosa.boardservice.dto.BoardDto;

import java.util.List;

@Mapper
public interface BoardMapper {

    // 전체 게시글 수 조회 (검색 조건 포함)
    int getTotalCount(@Param("searchValue") String searchValue);

    // 페이징된 게시글 목록 조회 (정렬 옵션 포함)
    List<BoardDto> selectPagedBoards(@Param("startRow") int startRow,
                                     @Param("pageSize") int pageSize,
                                     @Param("searchValue") String searchValue,
                                     @Param("orderBy") String orderBy);

    // 하위 호환성을 위한 기존 메서드 (오버로드)
    List<BoardDto> selectPagedBoards(@Param("startRow") int startRow,
                                     @Param("pageSize") int pageSize,
                                     @Param("searchValue") String searchValue);

    BoardDto getSelectBoardDetail(@Param("bno") Long bno);

    int updateViewCount(@Param("bno") Long bno);

    int updateBoard(BoardDto boardDto);

    boolean checkPassword(@Param("bno") Long bno, @Param("passwd") String passwd);

    // 게시글 삭제
    int deleteBoard(@Param("bno") Long bno, @Param("passwd") String passwd);

    void insertBoard(BoardDto boardDto);

    int updateNicknameByUserId(@Param("userId") String userId,
                               @Param("nickname") String nickname);

    int hideAllByUserId(@Param("userId") String userId);

    // 최신 게시글 조회
    List<BoardDto> selectRecentBoards(@Param("limit") int limit);

    // 인기 게시글 조회
    List<BoardDto> selectPopularBoards(@Param("limit") int limit);

    List<BoardDto> findRecentBoards(@Param("limit") int limit);
}