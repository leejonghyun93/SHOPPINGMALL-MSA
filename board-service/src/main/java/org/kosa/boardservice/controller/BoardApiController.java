package org.kosa.boardservice.controller;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.kosa.boardservice.JwtProvider;
import org.kosa.boardservice.dto.BoardDto;
import org.kosa.boardservice.dto.PageDto;
import org.kosa.boardservice.dto.PageRequestDto;
import org.kosa.boardservice.dto.UserDto;
import org.kosa.boardservice.service.BoardService;
import org.kosa.boardservice.service.UserServiceClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/board")
public class BoardApiController {

    private final BoardService boardService;
    private final RestTemplate restTemplate;
    private final JwtProvider jwtProvider;

    public BoardApiController(BoardService boardService, RestTemplate restTemplate, JwtProvider jwtProvider) {
        this.boardService = boardService;
        this.restTemplate = restTemplate;
        this.jwtProvider = jwtProvider;
    }

    // 페이징된 게시글 + 총 개수 반환 (PageDto)
    @GetMapping("/list")
    public PageDto<BoardDto> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String searchValue,
            @RequestParam(required = false) String sortBy
    ) {
        PageRequestDto requestDto = PageRequestDto.builder()
                .page(page)
                .size(size)
                .searchValue(searchValue)
                .sortBy(sortBy)
                .build();

        return boardService.getPagedBoards(requestDto);
    }


    // 게시글 상세 조회
    @GetMapping("/{bno}")
    public BoardDto getBoardDetail(@PathVariable Long bno) {
        return boardService.getBoardDetail(bno);
    }

    @PostMapping("/write")
    public ResponseEntity<String> writeBoard(@RequestHeader("Authorization") String authorization,
                                             @RequestBody BoardDto boardDto) {
        String token = authorization.replace("Bearer ", "");

        Claims claims = jwtProvider.parseToken(token); // 여기서 예외터지면 인증 실패임

        String userId = claims.getSubject();

        // 인증된 유저 ID를 글 작성에 사용
        boardDto.setWriterName(userId);
        boardService.saveBoard(boardDto);

        return ResponseEntity.ok("글 작성 성공");
    }
}

