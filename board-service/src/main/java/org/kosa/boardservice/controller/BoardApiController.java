package org.kosa.boardservice.controller;

import org.kosa.boardservice.dto.BoardDto;
import org.kosa.boardservice.service.BoardService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/board")
public class BoardApiController {

    private final BoardService boardService;

    public BoardApiController(BoardService boardService) {
        this.boardService = boardService;
    }

    // 게시글 목록 조회 (페이징 + 검색)
    @GetMapping("/list")
    public List<BoardDto> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String searchValue
    ) {
        return boardService.getPagedBoards(page, size, searchValue);
    }

    // 게시글 상세 조회
    @GetMapping("/{bno}")
    public BoardDto getBoardDetail(@PathVariable Long bno) {
        return boardService.getBoardDetail(bno);
    }

}
