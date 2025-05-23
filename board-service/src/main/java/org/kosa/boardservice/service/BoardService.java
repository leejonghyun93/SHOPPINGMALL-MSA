package org.kosa.boardservice.service;

import org.kosa.boardservice.dto.BoardDto;
import org.kosa.boardservice.dto.UserDto;  // UserDto도 만들어야 합니다.
import org.kosa.boardservice.mapper.BoardMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class BoardService {

    private final BoardMapper boardMapper;
    private final RestTemplate restTemplate;

    // 생성자에 RestTemplate 추가
    public BoardService(BoardMapper boardMapper, RestTemplate restTemplate) {
        this.boardMapper = boardMapper;
        this.restTemplate = restTemplate;
    }

    public int getTotalCount(String searchValue) {
        return boardMapper.getTotalCount(searchValue);
    }

    public List<BoardDto> getPagedBoards(int page, int size, String searchValue) {
        int startRow = (page - 1) * size;
        List<BoardDto> boards = boardMapper.selectPagedBoards(startRow, size, searchValue);

        // user-service에서 사용자 이름 받아와서 세팅
        for (BoardDto board : boards) {
            String writerId = board.getWriter();
            try {
                UserDto user = restTemplate.getForObject("http://user-service/users/" + writerId, UserDto.class);
                if (user != null) {
                    board.setWriterName(user.getName());
                } else {
                    board.setWriterName("알 수 없음");
                }
            } catch (Exception e) {
                board.setWriterName("오류");
            }
        }
        return boards;
    }

    public BoardDto getBoardDetail(Long bno) {
        boardMapper.updateViewCount(bno); // 조회수 증가
        BoardDto board = boardMapper.getSelectBoardDetail(bno);

        // 상세 조회 시에도 writerName 세팅
        if (board != null) {
            String writerId = board.getWriter();
            try {
                UserDto user = restTemplate.getForObject("http://user-service/users/" + writerId, UserDto.class);
                if (user != null) {
                    board.setWriterName(user.getName());
                } else {
                    board.setWriterName("알 수 없음");
                }
            } catch (Exception e) {
                board.setWriterName("오류");
            }
        }
        return board;
    }

    public void increaseViewCount(Long bno) {
        boardMapper.updateViewCount(bno);
    }

    public boolean updateBoard(BoardDto boardDto) {
        return boardMapper.updateBoard(boardDto) > 0;
    }

    public boolean deleteBoard(BoardDto boardDto) {
        return boardMapper.deleteBoard(boardDto) > 0;
    }

    public boolean checkPassword(Long bno, String passwd) {
        return boardMapper.checkPassword(bno, passwd) > 0;
    }

    public void insertBoard(BoardDto boardDto) {
        boardMapper.insert(boardDto);
    }
}
