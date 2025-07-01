package org.kosa.streamingservice.notificationservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kosa.notificationservice.dto.BroadcastScheduleDto;
import org.kosa.notificationservice.dto.NotificationCreateDto;
import org.kosa.notificationservice.dto.NotificationResponseDto;
import org.kosa.notificationservice.service.BroadcastService;
import org.kosa.notificationservice.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class NotificationController {

    private final NotificationService notificationService;
    private final BroadcastService broadcastService;

    // ================================
    // 🔥 헬스체크 (하나로 통합)
    // ================================

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> status = new HashMap<>();
        status.put("status", "UP");
        status.put("service", "NOTIFICATION-SERVICE");
        status.put("timestamp", LocalDateTime.now().toString());
        status.put("message", "알림 서비스 정상 동작 중");
        return ResponseEntity.ok(status);
    }

    // ================================
    // 🔥 방송 스케줄 관련
    // ================================

    /**
     * 방송 스케줄 조회 API
     */
    @GetMapping("/broadcasts/schedule")
    public ResponseEntity<List<BroadcastScheduleDto>> getBroadcastSchedule(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        log.info("방송 스케줄 조회 요청: {}", date);

        try {
            List<BroadcastScheduleDto> schedule = broadcastService.getBroadcastScheduleByDate(date);
            log.info("방송 스케줄 조회 성공: {} 개 시간대", schedule.size());
            return ResponseEntity.ok(schedule);
        } catch (Exception e) {
            log.error("방송 스케줄 조회 실패", e);
            return ResponseEntity.ok(Collections.emptyList());
        }
    }

    /**
     * 방송별 구독자 수 조회
     */
    @GetMapping("/broadcasts/{broadcastId}/subscribers/count")
    public ResponseEntity<Long> getBroadcastSubscriberCount(@PathVariable Long broadcastId) {
        long count = notificationService.getBroadcastSubscriberCount(broadcastId);
        return ResponseEntity.ok(count);
    }

    // ================================
    // 🔥 알림 생성 관련
    // ================================

    /**
     * 새 알림 생성
     */
    @PostMapping
    public ResponseEntity<NotificationResponseDto> createNotification(
            @Valid @RequestBody NotificationCreateDto createDto) {

        log.info("알림 생성 요청: broadcastId={}, userId={}",
                createDto.getBroadcastId(), createDto.getUserId());

        NotificationResponseDto response = notificationService.createNotification(createDto);
        return ResponseEntity.ok(response);
    }

    /**
     * 대량 알림 생성 (방송 서비스에서 호출)
     */
    @PostMapping("/bulk")
    public ResponseEntity<List<NotificationResponseDto>> createBulkNotifications(
            @Valid @RequestBody List<NotificationCreateDto> createDtos) {

        log.info("대량 알림 생성 요청: {}개", createDtos.size());

        List<NotificationResponseDto> responses = notificationService.createBulkNotifications(createDtos);
        return ResponseEntity.ok(responses);
    }

    /**
     * 즉시 알림 발송 (관리자용)
     */
    @PostMapping("/{notificationId}/send")
    public ResponseEntity<Void> sendNotificationNow(@PathVariable Long notificationId) {
        notificationService.sendNotificationNow(notificationId);
        return ResponseEntity.ok().build();
    }

    // ================================
    // 🔥 알림 조회 관련
    // ================================

    /**
     * 사용자별 알림 목록 조회 (페이징)
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<Page<NotificationResponseDto>> getUserNotifications(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<NotificationResponseDto> notifications =
                notificationService.getUserNotifications(userId, pageable);

        return ResponseEntity.ok(notifications);
    }

    /**
     * 읽지 않은 알림 목록 조회
     */
    @GetMapping("/users/{userId}/unread")
    public ResponseEntity<List<NotificationResponseDto>> getUnreadNotifications(
            @PathVariable String userId) {

        List<NotificationResponseDto> notifications =
                notificationService.getUnreadNotifications(userId);

        return ResponseEntity.ok(notifications);
    }

    // ================================
    // 🔥 헤더용 API (새로 추가)
    // ================================

    /**
     * 🔥 헤더용 - 읽지 않은 알림 개수 조회
     */
    @GetMapping("/unread-count")
    public ResponseEntity<?> getUnreadCount(@RequestParam String userId) {
        try {
            long count = notificationService.getUnreadCountByUserId(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("count", count);

            log.debug("읽지 않은 알림 개수 조회: userId={}, count={}", userId, count);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("읽지 않은 알림 개수 조회 실패: userId={}", userId, e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", "읽지 않은 알림 개수 조회 실패"));
        }
    }

    /**
     * 🔥 헤더용 - 최근 알림 목록 조회 (드롭다운용, 최대 10개)
     */
    @GetMapping("/recent")
    public ResponseEntity<?> getRecentNotifications(
            @RequestParam String userId,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<Map<String, Object>> notifications =
                    notificationService.getRecentNotificationsByUserId(userId, limit);

            log.debug("최근 알림 조회: userId={}, count={}", userId, notifications.size());
            return ResponseEntity.ok(notifications);
        } catch (Exception e) {
            log.error("최근 알림 조회 실패: userId={}", userId, e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", "최근 알림 조회 실패"));
        }
    }

    // ================================
    // 🔥 알림 읽음 처리 관련
    // ================================

    /**
     * 🔥 특정 알림 읽음 처리 (헤더용 + 기존용 통합)
     */
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<?> markAsRead(
            @PathVariable Long notificationId,
            @RequestParam String userId) {
        try {
            boolean success = notificationService.markAsReadByNotificationId(notificationId, userId);

            if (success) {
                log.info("알림 읽음 처리 성공: notificationId={}, userId={}", notificationId, userId);
                return ResponseEntity.ok(Map.of("message", "알림이 읽음 처리되었습니다."));
            } else {
                log.warn("알림 읽음 처리 실패 - 알림 없음: notificationId={}, userId={}", notificationId, userId);
                return ResponseEntity.status(404)
                        .body(Map.of("error", "알림을 찾을 수 없습니다."));
            }
        } catch (Exception e) {
            log.error("알림 읽음 처리 실패: notificationId={}, userId={}", notificationId, userId, e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", "알림 읽음 처리 실패"));
        }
    }

    /**
     * 🔥 모든 알림 읽음 처리 (헤더용)
     */
    @PatchMapping("/mark-all-read")
    public ResponseEntity<?> markAllAsRead(@RequestParam String userId) {
        try {
            int updatedCount = notificationService.markAllAsReadByUserId(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("updatedCount", updatedCount);
            response.put("message", "모든 알림이 읽음 처리되었습니다.");

            log.info("모든 알림 읽음 처리 완료: userId={}, count={}", userId, updatedCount);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("모든 알림 읽음 처리 실패: userId={}", userId, e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", "모든 알림 읽음 처리 실패"));
        }
    }

    // ================================
    // 🔥 알림 구독 관리
    // ================================

    /**
     * 알림 구독 취소
     */
    @DeleteMapping("/users/{userId}/broadcasts/{broadcastId}")
    public ResponseEntity<Void> unsubscribeNotification(
            @PathVariable String userId,
            @PathVariable Long broadcastId,
            @RequestParam String type) {

        notificationService.deleteNotification(userId, broadcastId, type);
        log.info("알림 구독 취소: userId={}, broadcastId={}, type={}", userId, broadcastId, type);
        return ResponseEntity.ok().build();
    }
}