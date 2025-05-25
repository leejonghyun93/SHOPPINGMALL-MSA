package org.kosa.boardservice.service;

import org.kosa.boardservice.dto.BoardDto;
import org.kosa.boardservice.dto.PageDto;
import org.kosa.boardservice.dto.PageRequestDto;
import org.kosa.boardservice.dto.UserDto;
import org.kosa.boardservice.mapper.BoardMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BoardService {

    private final BoardMapper boardMapper;
    private final RestTemplate restTemplate;

    // application.yml에서 URL 읽기
    @Value("${user-service.url}")
    private String userServiceUrl;

    @Value("${user-service-detail-url}")
    private String userServiceDetailUrl;

    public BoardService(BoardMapper boardMapper, RestTemplate restTemplate) {
        this.boardMapper = boardMapper;
        this.restTemplate = restTemplate;
    }

    public int getTotalCount(String searchValue) {
        return boardMapper.getTotalCount(searchValue);
    }

    public PageDto<BoardDto> getPagedBoards(PageRequestDto request) {
        int startRow = request.getStartRow();
        String orderBy = getOrderByClause(request.getSortBy());

        List<BoardDto> content = boardMapper.selectPagedBoards(startRow, request.getSize(), request.getSearchValue(), orderBy);

        // 사용자 ID 추출
        List<String> userIds = content.stream()
                .map(BoardDto::getWriter)
                .filter(id -> id != null && !id.isEmpty())
                .distinct()
                .collect(Collectors.toList());

        // 사용자 이름 매핑
        Map<String, String> userMap = fetchUserNames(userIds);

        for (BoardDto board : content) {
            board.setWriterName(userMap.getOrDefault(board.getWriter(), "알 수 없음"));
        }

        int totalElements = boardMapper.getTotalCount(request.getSearchValue());
        int totalPages = (int) Math.ceil((double) totalElements / request.getSize());

        return PageDto.<BoardDto>builder()
                .content(content)
                .page(request.getPage())
                .size(request.getSize())
                .totalElements(totalElements)
                .totalPages(totalPages)
                .build();
    }

    private Map<String, String> fetchUserNames(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            HttpEntity<List<String>> requestEntity = new HttpEntity<>(userIds);

            ResponseEntity<Map<String, String>> response = restTemplate.exchange(
                    userServiceUrl,
                    HttpMethod.POST,
                    requestEntity,
                    new ParameterizedTypeReference<>() {}
            );

            return response.getBody() != null ? response.getBody() : Collections.emptyMap();
        } catch (Exception e) {
            System.err.println("닉네임 요청 실패: " + e.getMessage());
            return Collections.emptyMap();
        }
    }

    private String getOrderByClause(String sortBy) {
        if (sortBy == null) sortBy = "latest";
        return switch (sortBy) {
            case "title" -> "b.title ASC";
            case "popular" -> "b.view_count DESC";
            default -> "b.bno DESC";
        };
    }

    public BoardDto getBoardDetail(Long bno) {
        boardMapper.updateViewCount(bno);
        BoardDto board = boardMapper.getSelectBoardDetail(bno);

        if (board != null) {
            board.setWriterName(fetchUserName(board.getWriter()));
        }

        return board;
    }

    /**
     * user-service 호출하여 사용자 이름 반환.
     * 실패 시 "오류" 반환, 사용자 없으면 "알 수 없음" 반환
     */
    private String fetchUserName(String writerId) {
        try {
            UserDto user = restTemplate.getForObject(userServiceDetailUrl + "/" + writerId, UserDto.class);
            if (user != null) {
                return user.getName();
            } else {
                return "알 수 없음";
            }
        } catch (Exception e) {
            System.err.println("User service 호출 실패: " + e.getMessage());
            return "오류";
        }
    }

    // 이하 기존 메서드 유지
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

    public void saveBoard(BoardDto boardDto) {
        System.out.println("저장할 글 작성자: " + boardDto.getWriterName());
        boardMapper.insertBoard(boardDto);
    }
}
