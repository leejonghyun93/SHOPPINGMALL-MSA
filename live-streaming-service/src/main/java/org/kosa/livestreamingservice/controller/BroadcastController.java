package org.kosa.livestreamingservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kosa.livestreamingservice.dto.BroadcastDto;
import org.kosa.livestreamingservice.dto.BroadcastScheduleDto;
import org.kosa.livestreamingservice.entity.BroadcastEntity;
import org.kosa.livestreamingservice.service.BroadcastService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Tag(name = "방송 API", description = "방송 관리 및 조회 API")
@Slf4j
@RestController
@RequestMapping("/api/broadcasts")
@RequiredArgsConstructor
// 🔥 CORS 설정 제거 - API Gateway에서 처리
public class BroadcastController {

    private final BroadcastService broadcastService;

    @Operation(summary = "방송 스케줄 조회 (알림용)", description = "날짜별 방송 스케줄을 조회합니다. (알림 서비스용)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = BroadcastScheduleDto.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/schedule/alarm")
    public ResponseEntity<List<BroadcastScheduleDto>> getBroadcastScheduleByDateForAlarm(
            @Parameter(description = "조회할 날짜", required = true, example = "2024-01-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        log.info("방송 스케줄 조회 요청 (알림용) - date: {}", date);

        try {
            List<BroadcastScheduleDto> schedule = broadcastService.getBroadcastScheduleByDate(date);
            return ResponseEntity.ok(schedule);

        } catch (Exception e) {
            log.error("방송 스케줄 조회 실패 - date: {}", date, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "방송 상태 업데이트", description = "방송의 상태를 업데이트합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "업데이트 성공")
    })
    @PutMapping("/status")
    public ResponseEntity<?> updateStatus(
            @Parameter(description = "업데이트할 방송 정보", required = true)
            @RequestBody BroadcastEntity broadCast) {
        broadcastService.updateStatus(broadCast);
        return ResponseEntity.ok().body(Map.of("result", "success"));
    }

    @Operation(summary = "라이브 방송 목록 조회", description = "진행 중인 라이브 방송 목록을 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/live")
    public ResponseEntity<List<BroadcastDto.Response>> getLiveBroadcasts(
            @Parameter(description = "방송 상태", example = "live")
            @RequestParam(required = false) String broadcast_status,
            @Parameter(description = "공개 여부 (1: 공개, 0: 비공개)", example = "1")
            @RequestParam(required = false) Integer is_public,
            @Parameter(description = "카테고리 ID", example = "1")
            @RequestParam(required = false) Integer category_id,
            @Parameter(description = "조회할 최대 개수", example = "100")
            @RequestParam(defaultValue = "100") int limit,
            @PageableDefault(size = 100) Pageable pageable) {

        log.info("라이브 방송 목록 조회 요청 - status: {}, public: {}, category: {}, limit: {}",
                broadcast_status, is_public, category_id, limit);

        try {
            List<BroadcastDto.Response> broadcasts = broadcastService.getLiveBroadcasts(
                    broadcast_status, is_public, category_id, limit);

            log.info("라이브 방송 {}개 조회 완료", broadcasts.size());
            return ResponseEntity.ok(broadcasts);

        } catch (Exception e) {
            log.error("라이브 방송 목록 조회 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "방송 상세 조회", description = "특정 방송의 상세 정보를 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = BroadcastDto.DetailResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "방송을 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/{broadcastId}")
    public ResponseEntity<BroadcastDto.DetailResponse> getBroadcastDetail(
            @Parameter(description = "방송 ID", required = true, example = "1")
            @PathVariable Long broadcastId) {

        log.info("방송 상세 조회 요청 - broadcastId: {}", broadcastId);

        try {
            BroadcastDto.DetailResponse broadcast = broadcastService.getBroadcastDetail(broadcastId);
            return ResponseEntity.ok(broadcast);

        } catch (IllegalArgumentException e) {
            log.warn("방송을 찾을 수 없음 - broadcastId: {}", broadcastId);
            return ResponseEntity.notFound().build();

        } catch (Exception e) {
            log.error("방송 상세 조회 실패 - broadcastId: {}", broadcastId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "카테고리별 방송 목록 조회", description = "특정 카테고리의 방송 목록을 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<BroadcastDto.Response>> getBroadcastsByCategory(
            @Parameter(description = "카테고리 ID", required = true, example = "1")
            @PathVariable Integer categoryId,
            @Parameter(description = "방송 상태", example = "live")
            @RequestParam(defaultValue = "live") String broadcast_status,
            @Parameter(description = "조회할 최대 개수", example = "50")
            @RequestParam(defaultValue = "50") int limit) {

        log.info("카테고리별 방송 목록 조회 - categoryId: {}, status: {}", categoryId, broadcast_status);

        try {
            List<BroadcastDto.Response> broadcasts = broadcastService.getBroadcastsByCategory(
                    categoryId, broadcast_status, limit);

            return ResponseEntity.ok(broadcasts);

        } catch (Exception e) {
            log.error("카테고리별 방송 목록 조회 실패 - categoryId: {}", categoryId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "방송자별 방송 목록 조회", description = "특정 방송자의 방송 목록을 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/broadcaster/{broadcasterId}")
    public ResponseEntity<List<BroadcastDto.Response>> getBroadcastsByBroadcaster(
            @Parameter(description = "방송자 ID", required = true, example = "broadcaster123")
            @PathVariable String broadcasterId,
            @PageableDefault(size = 20) Pageable pageable) {

        log.info("방송자별 방송 목록 조회 - broadcasterId: {}", broadcasterId);

        try {
            Page<BroadcastDto.Response> broadcasts = broadcastService.getBroadcastsByBroadcaster(
                    broadcasterId, pageable);

            return ResponseEntity.ok(broadcasts.getContent());

        } catch (Exception e) {
            log.error("방송자별 방송 목록 조회 실패 - broadcasterId: {}", broadcasterId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "방송 일정 조회", description = "캘린더용 방송 일정을 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/schedule")
    public ResponseEntity<List<BroadcastDto.ScheduleResponse>> getBroadcastSchedule(
            @Parameter(description = "조회할 날짜", required = true, example = "2024-01-01")
            @RequestParam String date) {

        log.info("방송 일정 조회 요청 - date: {}", date);

        try {
            List<BroadcastDto.ScheduleResponse> schedule = broadcastService.getBroadcastSchedule(date);
            return ResponseEntity.ok(schedule);

        } catch (Exception e) {
            log.error("방송 일정 조회 실패 - date: {}", date, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "방송 통계 조회", description = "전체 방송 통계를 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getBroadcastStats() {

        log.info("방송 통계 조회 요청");

        try {
            Map<String, Object> stats = broadcastService.getBroadcastStats();
            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            log.error("방송 통계 조회 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "방송 검색", description = "키워드로 방송을 검색합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "검색 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/search")
    public ResponseEntity<List<BroadcastDto.Response>> searchBroadcasts(
            @Parameter(description = "검색 키워드", required = true, example = "게임")
            @RequestParam String keyword,
            @Parameter(description = "조회할 최대 개수", example = "20")
            @RequestParam(defaultValue = "20") int limit) {

        log.info("방송 검색 요청 - keyword: {}, limit: {}", keyword, limit);

        try {
            List<BroadcastDto.Response> broadcasts = broadcastService.searchBroadcasts(keyword, limit);
            return ResponseEntity.ok(broadcasts);

        } catch (Exception e) {
            log.error("방송 검색 실패 - keyword: {}", keyword, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "추천 방송 목록 조회", description = "추천 방송 목록을 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/recommended")
    public ResponseEntity<List<BroadcastDto.Response>> getRecommendedBroadcasts(
            @Parameter(description = "조회할 최대 개수", example = "10")
            @RequestParam(defaultValue = "10") int limit) {

        log.info("추천 방송 목록 조회 요청 - limit: {}", limit);

        try {
            List<BroadcastDto.Response> broadcasts = broadcastService.getRecommendedBroadcasts(limit);
            return ResponseEntity.ok(broadcasts);

        } catch (Exception e) {
            log.error("추천 방송 목록 조회 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "방송 시청자 수 업데이트", description = "방송의 현재 시청자 수를 업데이트합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "업데이트 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "방송을 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PatchMapping("/{broadcastId}/viewers")
    public ResponseEntity<Void> updateViewerCount(
            @Parameter(description = "방송 ID", required = true, example = "1")
            @PathVariable Long broadcastId,
            @Parameter(description = "시청자 수 정보", required = true)
            @RequestBody Map<String, Integer> request) {

        Integer viewerCount = request.get("current_viewers");
        log.info("방송 시청자 수 업데이트 - broadcastId: {}, viewers: {}", broadcastId, viewerCount);

        try {
            broadcastService.updateViewerCount(broadcastId, viewerCount);
            return ResponseEntity.ok().build();

        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();

        } catch (Exception e) {
            log.error("방송 시청자 수 업데이트 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "방송 좋아요 수 업데이트", description = "방송의 좋아요 수를 업데이트합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "업데이트 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "방송을 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PatchMapping("/{broadcastId}/likes")
    public ResponseEntity<Void> updateLikeCount(
            @Parameter(description = "방송 ID", required = true, example = "1")
            @PathVariable Long broadcastId,
            @Parameter(description = "좋아요 수 정보", required = true)
            @RequestBody Map<String, Integer> request) {

        Integer likeCount = request.get("like_count");
        log.info("방송 좋아요 수 업데이트 - broadcastId: {}, likes: {}", broadcastId, likeCount);

        try {
            broadcastService.updateLikeCount(broadcastId, likeCount);
            return ResponseEntity.ok().build();

        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();

        } catch (Exception e) {
            log.error("방송 좋아요 수 업데이트 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "헬스 체크", description = "라이브 스트리밍 서비스의 상태를 확인합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "서비스 정상")
    })
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "live-streaming-service",
                "timestamp", String.valueOf(System.currentTimeMillis())
        ));
    }

    @Operation(summary = "전체 방송 조회 (디버그용)", description = "디버그용 - 모든 방송을 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/debug/all")
    public ResponseEntity<List<BroadcastDto.Response>> getAllBroadcasts(
            @Parameter(description = "조회할 최대 개수", example = "20")
            @RequestParam(defaultValue = "20") int limit) {

        log.info("디버그용 전체 방송 조회 - limit: {}", limit);

        try {
            // 모든 상태의 방송 조회
            List<BroadcastDto.Response> broadcasts = broadcastService.getLiveBroadcasts(
                    null, null, null, limit);

            return ResponseEntity.ok(broadcasts);

        } catch (Exception e) {
            log.error("디버그용 전체 방송 조회 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}