package org.kosa.notificationservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kosa.notificationservice.dto.NotificationResponseDto;
import org.kosa.notificationservice.service.NotificationSubscriptionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 방송 알림 구독 관리 컨트롤러
 */
@RestController
@RequestMapping("/api/notifications/subscriptions")
@RequiredArgsConstructor
@Slf4j
public class NotificationSubscriptionController {

    private final NotificationSubscriptionService subscriptionService;

    /**
     * 🔔 방송 시작 알림 구독 신청
     */
    @PostMapping("/broadcast-start")
    public ResponseEntity<NotificationResponseDto> subscribeBroadcastStart(
            @RequestParam Long userId,
            @RequestParam Long broadcastId) {

        log.info("방송 시작 알림 구독 요청: userId={}, broadcastId={}", userId, broadcastId);

        NotificationResponseDto response = subscriptionService.subscribeBroadcastStart(userId, broadcastId);
        return ResponseEntity.ok(response);
    }

    /**
     * ❌ 방송 알림 구독 취소
     */
    @DeleteMapping
    public ResponseEntity<Void> unsubscribeBroadcast(
            @RequestParam Long userId,
            @RequestParam Long broadcastId,
            @RequestParam(defaultValue = "BROADCAST_START") String type) {

        log.info("방송 알림 구독 취소 요청: userId={}, broadcastId={}, type={}", userId, broadcastId, type);

        subscriptionService.unsubscribeBroadcast(userId, broadcastId, type);
        return ResponseEntity.ok().build();
    }

    /**
     * 📋 사용자의 구독 중인 방송 목록 조회
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<List<NotificationResponseDto>> getUserSubscriptions(@PathVariable Long userId) {

        List<NotificationResponseDto> subscriptions = subscriptionService.getUserSubscriptions(userId);
        return ResponseEntity.ok(subscriptions);
    }

    /**
     * 🎯 특정 방송의 구독자 수 조회
     */
    @GetMapping("/broadcasts/{broadcastId}/count")
    public ResponseEntity<Long> getBroadcastSubscriberCount(@PathVariable Long broadcastId) {

        long count = subscriptionService.getBroadcastSubscriberCount(broadcastId);
        return ResponseEntity.ok(count);
    }

    /**
     * 🔥 방송 시작시 구독자들에게 대량 알림 생성 (내부 API - Live Streaming Service에서 호출)
     */
    @PostMapping("/broadcasts/{broadcastId}/start-notifications")
    public ResponseEntity<List<NotificationResponseDto>> createBroadcastStartNotifications(
            @PathVariable Long broadcastId) {

        log.info("방송 시작 - 구독자 알림 생성 요청: broadcastId={}", broadcastId);

        List<NotificationResponseDto> notifications =
                subscriptionService.createBroadcastStartNotifications(broadcastId);

        return ResponseEntity.ok(notifications);
    }
}