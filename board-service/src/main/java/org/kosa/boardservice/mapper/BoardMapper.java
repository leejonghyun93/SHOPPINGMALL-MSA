package org.kosa.boardservice.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.kosa.boardservice.dto.BoardDto;

import java.util.List;

@Mapper
public interface BoardMapper {

    int getTotalCount(@Param("searchValue") String searchValue);

    List<BoardDto> selectPagedBoards(@Param("startRow") int startRow,
                                     @Param("pageSize") int pageSize,
                                     @Param("searchValue") String searchValue);

    BoardDto getSelectBoardDetail(@Param("bno") Long bno);

    int updateViewCount(@Param("bno") Long bno);

    int updateBoard(BoardDto boardDto);

    int deleteBoard(BoardDto boardDto);

    int checkPassword(@Param("bno") Long bno, @Param("passwd") String passwd);

    void insert(BoardDto boardDto);
}