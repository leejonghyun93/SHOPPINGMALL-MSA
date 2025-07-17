package org.kosa.livestreamingservice.controller.alarm;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kosa.livestreamingservice.dto.alarm.NotificationResponseDto;
import org.kosa.livestreamingservice.service.alarm.NotificationSubscriptionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "알림 구독 API", description = "방송 알림 구독 관리 API")
@RestController
@RequestMapping("/api/notifications/subscriptions")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class NotificationSubscriptionController {

    private final NotificationSubscriptionService subscriptionService;

    @Operation(summary = "방송 시작 알림 구독", description = "특정 방송의 시작 알림을 구독합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "구독 성공",
                    content = @Content(schema = @Schema(implementation = NotificationResponseDto.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 파라미터"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PostMapping("/broadcast-start")
    public ResponseEntity<?> subscribeBroadcastStart(
            @Parameter(description = "사용자 ID", required = true, example = "user123")
            @RequestParam String userId,
            @Parameter(description = "방송 ID", required = true, example = "1")
            @RequestParam String broadcastId) {

        log.info("방송 시작 알림 구독 요청: userId={}, broadcastId={}", userId, broadcastId);

        try {
            Long broadcastIdLong = parseToLong(broadcastId, "broadcastId");

            NotificationResponseDto response = subscriptionService
                    .subscribeBroadcastStart(userId, broadcastIdLong);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("잘못된 파라미터: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "INVALID_PARAMETER", "message", e.getMessage()));
        } catch (Exception e) {
            log.error("방송 구독 처리 중 오류 발생: ", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "INTERNAL_ERROR", "message", "구독 처리 중 오류가 발생했습니다."));
        }
    }

    @Operation(summary = "방송 알림 구독 취소", description = "특정 방송의 알림 구독을 취소합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "구독 취소 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 파라미터"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @DeleteMapping
    public ResponseEntity<?> unsubscribeBroadcast(
            @Parameter(description = "사용자 ID", required = true, example = "user123")
            @RequestParam String userId,
            @Parameter(description = "방송 ID", required = true, example = "1")
            @RequestParam String broadcastId,
            @Parameter(description = "알림 타입", example = "BROADCAST_START")
            @RequestParam(defaultValue = "BROADCAST_START") String type) {

        log.info("방송 알림 구독 취소 요청: userId={}, broadcastId={}, type={}", userId, broadcastId, type);

        try {
            Long broadcastIdLong = parseToLong(broadcastId, "broadcastId");

            subscriptionService.unsubscribeBroadcast(userId, broadcastIdLong, type);
            return ResponseEntity.ok().build();

        } catch (IllegalArgumentException e) {
            log.error("잘못된 파라미터: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "INVALID_PARAMETER", "message", e.getMessage()));
        } catch (Exception e) {
            log.error("방송 구독 취소 처리 중 오류 발생: ", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "INTERNAL_ERROR", "message", "구독 취소 처리 중 오류가 발생했습니다."));
        }
    }

    @Operation(summary = "사용자 구독 목록 조회", description = "사용자가 구독 중인 방송 목록을 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 사용자 ID")
    })
    @GetMapping("/users/{userId}")
    public ResponseEntity<?> getUserSubscriptions(
            @Parameter(description = "사용자 ID", required = true, example = "user123")
            @PathVariable String userId) {

        try {
            List<NotificationResponseDto> subscriptions = subscriptionService.getUserSubscriptions(userId);
            return ResponseEntity.ok(subscriptions);

        } catch (IllegalArgumentException e) {
            log.error("잘못된 사용자 ID: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "INVALID_USER_ID", "message", e.getMessage()));
        }
    }

    @Operation(summary = "방송 구독자 수 조회", description = "특정 방송의 구독자 수를 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = Long.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 방송 ID")
    })
    @GetMapping("/broadcasts/{broadcastId}/count")
    public ResponseEntity<?> getBroadcastSubscriberCount(
            @Parameter(description = "방송 ID", required = true, example = "1")
            @PathVariable String broadcastId) {

        try {
            Long broadcastIdLong = parseToLong(broadcastId, "broadcastId");
            long count = subscriptionService.getBroadcastSubscriberCount(broadcastIdLong);
            return ResponseEntity.ok(count);

        } catch (IllegalArgumentException e) {
            log.error("잘못된 방송 ID: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "INVALID_BROADCAST_ID", "message", e.getMessage()));
        }
    }

    @Operation(summary = "방송 시작 알림 생성", description = "방송 시작시 구독자들에게 대량 알림을 생성합니다. (내부 API)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "알림 생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 방송 ID")
    })
    @PostMapping("/broadcasts/{broadcastId}/start-notifications")
    public ResponseEntity<?> createBroadcastStartNotifications(
            @Parameter(description = "방송 ID", required = true, example = "1")
            @PathVariable String broadcastId) {

        log.info("방송 시작 - 구독자 알림 생성 요청: broadcastId={}", broadcastId);

        try {
            Long broadcastIdLong = parseToLong(broadcastId, "broadcastId");
            List<NotificationResponseDto> notifications =
                    subscriptionService.createBroadcastStartNotifications(broadcastIdLong);

            return ResponseEntity.ok(notifications);

        } catch (IllegalArgumentException e) {
            log.error("잘못된 방송 ID: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "INVALID_BROADCAST_ID", "message", e.getMessage()));
        }
    }

    /**
     * 안전한 Long 파싱 유틸리티 메서드 (broadcastId용)
     */
    private Long parseToLong(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + "이(가) 비어있습니다.");
        }

        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    String.format("%s '%s'은(는) 유효한 숫자가 아닙니다.", fieldName, value));
        }
    }
}