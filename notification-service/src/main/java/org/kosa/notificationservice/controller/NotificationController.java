package org.kosa.notificationservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kosa.notificationservice.dto.NotificationCreateDto;
import org.kosa.notificationservice.dto.NotificationResponseDto;
import org.kosa.notificationservice.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class NotificationController {

    private final NotificationService notificationService;

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
     * 사용자별 알림 목록 조회 (페이징)
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<Page<NotificationResponseDto>> getUserNotifications(
            @PathVariable Long userId,
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
            @PathVariable Long userId) {

        List<NotificationResponseDto> notifications =
                notificationService.getUnreadNotifications(userId);

        return ResponseEntity.ok(notifications);
    }

    /**
     * 읽지 않은 알림 개수 조회
     */
    @GetMapping("/users/{userId}/unread/count")
    public ResponseEntity<Long> getUnreadCount(@PathVariable Long userId) {
        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(count);
    }

    /**
     * 알림 읽음 처리
     */
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long notificationId,
            @RequestParam Long userId) {

        notificationService.markAsRead(notificationId, userId);
        return ResponseEntity.ok().build();
    }

    /**
     * 알림 구독 취소
     */
    @DeleteMapping("/users/{userId}/broadcasts/{broadcastId}")
    public ResponseEntity<Void> unsubscribeNotification(
            @PathVariable Long userId,
            @PathVariable Long broadcastId,
            @RequestParam String type) {

        notificationService.deleteNotification(userId, broadcastId, type);
        return ResponseEntity.ok().build();
    }

    /**
     * 즉시 알림 발송 (관리자용)
     */
    @PostMapping("/{notificationId}/send")
    public ResponseEntity<Void> sendNotificationNow(@PathVariable Long notificationId) {
        notificationService.sendNotificationNow(notificationId);
        return ResponseEntity.ok().build();
    }

    /**
     * 방송별 구독자 수 조회
     */
    @GetMapping("/broadcasts/{broadcastId}/subscribers/count")
    public ResponseEntity<Long> getBroadcastSubscriberCount(@PathVariable Long broadcastId) {
        long count = notificationService.getBroadcastSubscriberCount(broadcastId);
        return ResponseEntity.ok(count);
    }


}