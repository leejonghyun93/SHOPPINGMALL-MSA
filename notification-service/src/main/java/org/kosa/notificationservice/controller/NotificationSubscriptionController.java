package org.kosa.notificationservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kosa.notificationservice.dto.NotificationResponseDto;
import org.kosa.notificationservice.service.NotificationSubscriptionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 방송 알림 구독 관리 컨트롤러
 */
@RestController
@RequestMapping("/api/notifications/subscriptions")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class NotificationSubscriptionController {

    private final NotificationSubscriptionService subscriptionService;

    /**
     * 🔔 방송 시작 알림 구독 신청 - 문자열/숫자 ID 모두 지원
     */
    @PostMapping("/broadcast-start")
    public ResponseEntity<?> subscribeBroadcastStart(
            @RequestParam String userId,
            @RequestParam String broadcastId) {

        log.info("방송 시작 알림 구독 요청: userId={}, broadcastId={}", userId, broadcastId);

        try {
            // ✅ broadcastId는 숫자여야 함
            Long broadcastIdLong = parseToLong(broadcastId, "broadcastId");

            // ✅ userId는 문자열/숫자 모두 지원
            NotificationResponseDto response = subscriptionService
                    .subscribeBroadcastStart(userId, broadcastIdLong);  // String userId 전달

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

    /**
     * ❌ 방송 알림 구독 취소 - 문자열/숫자 ID 모두 지원
     */
    @DeleteMapping
    public ResponseEntity<?> unsubscribeBroadcast(
            @RequestParam String userId,
            @RequestParam String broadcastId,
            @RequestParam(defaultValue = "BROADCAST_START") String type) {

        log.info("방송 알림 구독 취소 요청: userId={}, broadcastId={}, type={}", userId, broadcastId, type);

        try {
            Long broadcastIdLong = parseToLong(broadcastId, "broadcastId");

            // ✅ userId는 문자열 그대로 전달
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

    /**
     * 📋 사용자의 구독 중인 방송 목록 조회 - 문자열/숫자 ID 모두 지원
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<?> getUserSubscriptions(@PathVariable String userId) {

        try {
            // ✅ userId는 문자열 그대로 전달
            List<NotificationResponseDto> subscriptions = subscriptionService.getUserSubscriptions(userId);
            return ResponseEntity.ok(subscriptions);

        } catch (IllegalArgumentException e) {
            log.error("잘못된 사용자 ID: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "INVALID_USER_ID", "message", e.getMessage()));
        }
    }

    /**
     * 🎯 특정 방송의 구독자 수 조회
     */
    @GetMapping("/broadcasts/{broadcastId}/count")
    public ResponseEntity<?> getBroadcastSubscriberCount(@PathVariable String broadcastId) {

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

    /**
     * 🔥 방송 시작시 구독자들에게 대량 알림 생성 (내부 API)
     */
    @PostMapping("/broadcasts/{broadcastId}/start-notifications")
    public ResponseEntity<?> createBroadcastStartNotifications(@PathVariable String broadcastId) {

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
     * ✅ 안전한 Long 파싱 유틸리티 메서드 (broadcastId용)
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