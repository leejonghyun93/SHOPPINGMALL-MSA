package org.kosa.livestreamingservice.controller;

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

@Slf4j
@RestController
@RequestMapping("/api/broadcasts")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class BroadcastController {

    private final BroadcastService broadcastService;

    // ============ 기존 알림 서비스용 API ============

    /**
     * 날짜별 방송 스케줄 조회 (알림 서비스용) - 기존
     */
    @GetMapping("/schedule/alarm")
    public ResponseEntity<List<BroadcastScheduleDto>> getBroadcastScheduleByDateForAlarm(
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
    @PutMapping("/status")
    public ResponseEntity<?> updateStatus(@RequestBody BroadcastEntity broadCast) {
        broadcastService.updateStatus(broadCast);
        return ResponseEntity.ok().body(Map.of("result", "success"));
    }

    // ============ 프론트엔드용 API ============

    /**
     * 진행 중인 라이브 방송 목록 조회 (프론트엔드용)
     */
    @GetMapping("/live")
    public ResponseEntity<List<BroadcastDto.Response>> getLiveBroadcasts(
            @RequestParam(required = false) String broadcast_status,
            @RequestParam(required = false) Integer is_public,
            @RequestParam(required = false) Integer category_id,
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

    /**
     * 방송 상세 조회
     */
    @GetMapping("/{broadcastId}")
    public ResponseEntity<BroadcastDto.DetailResponse> getBroadcastDetail(
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

    /**
     * 카테고리별 방송 목록 조회
     */
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<BroadcastDto.Response>> getBroadcastsByCategory(
            @PathVariable Integer categoryId,
            @RequestParam(defaultValue = "live") String broadcast_status,
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

    /**
     * 방송자별 방송 목록 조회
     */
    @GetMapping("/broadcaster/{broadcasterId}")
    public ResponseEntity<List<BroadcastDto.Response>> getBroadcastsByBroadcaster(
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

    /**
     * 방송 일정 조회 (캘린더용) - 프론트엔드용
     */
    @GetMapping("/schedule")
    public ResponseEntity<List<BroadcastDto.ScheduleResponse>> getBroadcastSchedule(
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

    /**
     * 방송 통계 조회
     */
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

    /**
     * 방송 검색
     */
    @GetMapping("/search")
    public ResponseEntity<List<BroadcastDto.Response>> searchBroadcasts(
            @RequestParam String keyword,
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

    /**
     * 추천 방송 목록 조회
     */
    @GetMapping("/recommended")
    public ResponseEntity<List<BroadcastDto.Response>> getRecommendedBroadcasts(
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

    /**
     * 방송 시청자 수 업데이트
     */
    @PatchMapping("/{broadcastId}/viewers")
    public ResponseEntity<Void> updateViewerCount(
            @PathVariable Long broadcastId,
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

    /**
     * 방송 좋아요 수 업데이트
     */
    @PatchMapping("/{broadcastId}/likes")
    public ResponseEntity<Void> updateLikeCount(
            @PathVariable Long broadcastId,
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

    /**
     * 헬스 체크
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "live-streaming-service",
                "timestamp", String.valueOf(System.currentTimeMillis())
        ));
    }

    /**
     * 디버그용 - 모든 방송 조회
     */
    @GetMapping("/debug/all")
    public ResponseEntity<List<BroadcastDto.Response>> getAllBroadcasts(
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