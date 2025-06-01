package org.kosa.boardservice.controller;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.kosa.boardservice.JwtUtil;
import org.kosa.boardservice.dto.BoardDto;
import org.kosa.boardservice.dto.PageDto;
import org.kosa.boardservice.dto.PageRequestDto;
import org.kosa.boardservice.dto.UserDto;
import org.kosa.boardservice.service.BoardService;
import org.kosa.boardservice.service.UserServiceClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/board")
public class BoardApiController {

    private final BoardService boardService;
    private final JwtUtil jwtProvider;
    private final UserServiceClient userServiceClient;

    public BoardApiController(BoardService boardService,
                              JwtUtil jwtProvider,
                              UserServiceClient userServiceClient) {
        this.boardService = boardService;
        this.jwtProvider = jwtProvider;
        this.userServiceClient = userServiceClient;
    }

    // 게시글 리스트 조회
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
        System.out.println(">> 게시글 상세 요청 bno: " + bno);
        return boardService.getBoardDetail(bno);
    }

    // 게시글 작성 - JWT 토큰 헤더에서 추출하여 검증 후 사용자 정보 조회 및 게시글 저장
    @PostMapping("/write")
    public ResponseEntity<String> writeBoard(@RequestHeader("Authorization") String authorization,
                                             @RequestBody BoardDto boardDto) {
        try {
            System.out.println("게시글 작성 요청 - Authorization: " + authorization);

            // 1. Bearer 토큰에서 토큰 문자열만 추출
            if (authorization == null || !authorization.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authorization 헤더가 잘못되었습니다");
            }
            String token = authorization.substring(7);
            System.out.println("추출된 토큰: " + token);

            // 2. 토큰 유효성 검증
            if (!jwtProvider.validateToken(token)) {
                System.err.println("토큰 검증 실패");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("유효하지 않은 토큰입니다");
            }

            // 3. 토큰에서 사용자 아이디 추출
            Claims claims = jwtProvider.parseToken(token);
            String userId = claims.getSubject();
            System.out.println("토큰에서 추출된 사용자 ID: " + userId);

            if (userId == null || userId.isEmpty()) {
                System.err.println("토큰에서 사용자 ID를 찾을 수 없습니다");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("사용자 정보를 찾을 수 없습니다");
            }

            // 4. 사용자 서비스에서 사용자 정보 조회
            UserDto user = userServiceClient.getUserByUserId(userId);
            System.out.println("조회된 사용자 정보: " + user.getName());

            // 5. 게시글 작성자 이름 설정 및 저장
            boardDto.setWriterName(user.getName());
            boardService.saveBoard(boardDto);

            System.out.println("게시글 작성 완료");
            return ResponseEntity.ok("글 작성 성공");

        } catch (Exception e) {
            System.err.println("게시글 작성 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("글 작성 실패: " + e.getMessage());
        }
    }

    // 인증 확인용 테스트 엔드포인트
    @GetMapping("/auth-test")
    public ResponseEntity<String> authTest() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            return ResponseEntity.ok("인증됨: " + auth.getName());
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("인증되지 않음");
        }
    }

    @PutMapping("/{bno}")
    public ResponseEntity<String> updateBoard(@PathVariable Long bno,
                                              @RequestHeader("Authorization") String authorization,
                                              @RequestBody BoardDto boardDto) {
        try {
            // 1. JWT 토큰 유효성 및 사용자 ID 추출
            if (authorization == null || !authorization.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authorization 헤더가 필요합니다.");
            }
            String token = authorization.substring(7);
            if (!jwtProvider.validateToken(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("유효하지 않은 토큰입니다.");
            }
            Claims claims = jwtProvider.parseToken(token);
            String userId = claims.getSubject();

            // 2. 현재 게시글의 작성자 정보 가져오기
            BoardDto originalBoard = boardService.getBoardDetail(Long.valueOf(bno));
            if (originalBoard == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("게시글을 찾을 수 없습니다.");
            }

            // 3. 작성자와 로그인 사용자 비교
            UserDto user = userServiceClient.getUserByUserId(userId);
            if (!originalBoard.getWriterName().equals(user.getName())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("수정 권한이 없습니다.");
            }

            // 4. 수정 진행
            boardDto.setBno(bno.longValue());
            boardService.updateBoard(boardDto);
            return ResponseEntity.ok("글 수정 성공");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("글 수정 실패: " + e.getMessage());
        }
    }
    @DeleteMapping("/{bno}")
    public ResponseEntity<String> deleteBoardWithPassword(@PathVariable Long bno,
                                                          @RequestParam String passwd,
                                                          @RequestHeader("Authorization") String authorization) {
        try {
            if (authorization == null || !authorization.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authorization 헤더가 잘못되었습니다");
            }

            String token = authorization.substring(7); // Bearer 제거
            if (!jwtProvider.validateToken(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("유효하지 않은 토큰입니다");
            }

            Claims claims = jwtProvider.parseToken(token);
            String userId = claims.getSubject();
            if (userId == null || userId.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("사용자 정보를 찾을 수 없습니다");
            }

            BoardDto board = boardService.getBoardDetail(bno);
            if (board == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("게시글을 찾을 수 없습니다");
            }


            // 현재 writer 필드는 'writerName' 인 것 같으니, 실제 사용자 id 필드로 변경 필요
            if (!userId.equals(board.getWriter())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("삭제 권한이 없습니다");
            }

            // 비밀번호 검증은 service 단에서 처리 (boardService.deleteBoardWithPassword)
            boolean deleted = boardService.deleteBoardWithPassword(bno, passwd);
            if (!deleted) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("비밀번호가 일치하지 않습니다");
            }

            return ResponseEntity.ok("게시글 삭제 성공");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("게시글 삭제 실패: " + e.getMessage());
        }
    }

    @GetMapping("/chart/recent")
    public ResponseEntity<List<BoardDto>> getRecentBoards(
            @RequestParam(defaultValue = "5") int limit) {
        try {
            List<BoardDto> recentBoards = boardService.getRecentBoards(limit);
            return ResponseEntity.ok(recentBoards);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    // 인기 게시글 리스트 (예: 조회수 높은 상위 5개)
    @GetMapping("/chart/popular")
    public ResponseEntity<List<BoardDto>> getPopularBoards(
            @RequestParam(defaultValue = "5") int limit) {
        try {
            List<BoardDto> popularBoards = boardService.getPopularBoards(limit);
            return ResponseEntity.ok(popularBoards);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }
    @GetMapping("/recentList")
    public List<BoardDto> getRecentBoardList(@RequestParam(defaultValue = "10") int limit) {
        return boardService.getRecentBoardList(limit);
    }
}
