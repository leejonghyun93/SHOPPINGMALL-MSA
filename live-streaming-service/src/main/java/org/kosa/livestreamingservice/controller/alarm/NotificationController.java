package org.kosa.livestreamingservice.controller.alarm;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kosa.livestreamingservice.dto.BroadcastScheduleDto;
import org.kosa.livestreamingservice.dto.alarm.NotificationCreateDto;
import org.kosa.livestreamingservice.dto.alarm.NotificationResponseDto;
import org.kosa.livestreamingservice.service.BroadcastService;
import org.kosa.livestreamingservice.service.alarm.NotificationService;
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

@Tag(name = "알림 API", description = "알림 관리 및 방송 알림 API")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class NotificationController {

    private final NotificationService notificationService;
    private final BroadcastService broadcastService;

    @Operation(summary = "헬스체크", description = "알림 서비스의 상태를 확인합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "서비스 정상")
    })
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> status = new HashMap<>();
        status.put("status", "UP");
        status.put("service", "NOTIFICATION-SERVICE");
        status.put("timestamp", LocalDateTime.now().toString());
        status.put("message", "알림 서비스 정상 동작 중");
        return ResponseEntity.ok(status);
    }

    @Operation(summary = "방송 스케줄 조회", description = "특정 날짜의 방송 스케줄을 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = BroadcastScheduleDto.class)))
    })
    @GetMapping("/broadcasts/schedule")
    public ResponseEntity<List<BroadcastScheduleDto>> getBroadcastSchedule(
            @Parameter(description = "조회할 날짜", required = true, example = "2024-01-01")
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

    @Operation(summary = "방송별 구독자 수 조회", description = "특정 방송의 구독자 수를 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = Long.class)))
    })
    @GetMapping("/broadcasts/{broadcastId}/subscribers/count")
    public ResponseEntity<Long> getBroadcastSubscriberCount(
            @Parameter(description = "방송 ID", required = true, example = "1")
            @PathVariable Long broadcastId) {
        long count = notificationService.getBroadcastSubscriberCount(broadcastId);
        return ResponseEntity.ok(count);
    }

    @Operation(summary = "알림 생성", description = "새로운 알림을 생성합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "생성 성공",
                    content = @Content(schema = @Schema(implementation = NotificationResponseDto.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    @PostMapping
    public ResponseEntity<NotificationResponseDto> createNotification(
            @Parameter(description = "알림 생성 정보", required = true)
            @Valid @RequestBody NotificationCreateDto createDto) {

        log.info("알림 생성 요청: broadcastId={}, userId={}",
                createDto.getBroadcastId(), createDto.getUserId());

        NotificationResponseDto response = notificationService.createNotification(createDto);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "대량 알림 생성", description = "여러 알림을 한번에 생성합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "생성 성공")
    })
    @PostMapping("/bulk")
    public ResponseEntity<List<NotificationResponseDto>> createBulkNotifications(
            @Parameter(description = "대량 알림 생성 정보", required = true)
            @Valid @RequestBody List<NotificationCreateDto> createDtos) {

        log.info("대량 알림 생성 요청: {}개", createDtos.size());

        List<NotificationResponseDto> responses = notificationService.createBulkNotifications(createDtos);
        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "즉시 알림 발송", description = "특정 알림을 즉시 발송합니다. (관리자용)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "발송 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "알림을 찾을 수 없음")
    })
    @PostMapping("/{notificationId}/send")
    public ResponseEntity<Void> sendNotificationNow(
            @Parameter(description = "알림 ID", required = true, example = "1")
            @PathVariable Long notificationId) {
        notificationService.sendNotificationNow(notificationId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "사용자별 알림 목록 조회", description = "특정 사용자의 알림 목록을 페이징으로 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping("/users/{userId}")
    public ResponseEntity<Page<NotificationResponseDto>> getUserNotifications(
            @Parameter(description = "사용자 ID", required = true, example = "user123")
            @PathVariable String userId,
            @Parameter(description = "페이지 번호", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<NotificationResponseDto> notifications =
                notificationService.getUserNotifications(userId, pageable);

        return ResponseEntity.ok(notifications);
    }

    @Operation(summary = "읽지 않은 알림 목록 조회", description = "사용자의 읽지 않은 알림 목록을 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping("/users/{userId}/unread")
    public ResponseEntity<List<NotificationResponseDto>> getUnreadNotifications(
            @Parameter(description = "사용자 ID", required = true, example = "user123")
            @PathVariable String userId) {

        List<NotificationResponseDto> notifications =
                notificationService.getUnreadNotifications(userId);

        return ResponseEntity.ok(notifications);
    }

    @Operation(summary = "읽지 않은 알림 개수 조회", description = "헤더용 - 사용자의 읽지 않은 알림 개수를 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/unread-count")
    public ResponseEntity<?> getUnreadCount(
            @Parameter(description = "사용자 ID", required = true, example = "user123")
            @RequestParam String userId) {
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

    @Operation(summary = "최근 알림 목록 조회", description = "헤더 드롭다운용 - 사용자의 최근 알림 목록을 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/recent")
    public ResponseEntity<?> getRecentNotifications(
            @Parameter(description = "사용자 ID", required = true, example = "user123")
            @RequestParam String userId,
            @Parameter(description = "조회할 최대 개수", example = "10")
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

    @Operation(summary = "알림 읽음 처리", description = "특정 알림을 읽음 상태로 변경합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "처리 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "알림을 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<?> markAsRead(
            @Parameter(description = "알림 ID", required = true, example = "1")
            @PathVariable Long notificationId,
            @Parameter(description = "사용자 ID", required = true, example = "user123")
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

    @Operation(summary = "모든 알림 읽음 처리", description = "사용자의 모든 알림을 읽음 상태로 변경합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "처리 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PatchMapping("/mark-all-read")
    public ResponseEntity<?> markAllAsRead(
            @Parameter(description = "사용자 ID", required = true, example = "user123")
            @RequestParam String userId) {
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

    @Operation(summary = "알림 구독 취소", description = "특정 방송의 알림 구독을 취소합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "취소 성공")
    })
    @DeleteMapping("/users/{userId}/broadcasts/{broadcastId}")
    public ResponseEntity<Void> unsubscribeNotification(
            @Parameter(description = "사용자 ID", required = true, example = "user123")
            @PathVariable String userId,
            @Parameter(description = "방송 ID", required = true, example = "1")
            @PathVariable Long broadcastId,
            @Parameter(description = "알림 타입", required = true, example = "BROADCAST_START")
            @RequestParam String type) {

        notificationService.deleteNotification(userId, broadcastId, type);
        log.info("알림 구독 취소: userId={}, broadcastId={}, type={}", userId, broadcastId, type);
        return ResponseEntity.ok().build();
    }
}