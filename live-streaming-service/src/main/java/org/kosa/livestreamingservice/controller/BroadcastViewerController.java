package org.kosa.livestreamingservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kosa.livestreamingservice.dto.BroadcastDto;
import org.kosa.livestreamingservice.dto.ProductDto;
import org.kosa.livestreamingservice.service.BroadcastViewerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/broadcast")
@RequiredArgsConstructor
@Slf4j
// 🔥 @CrossOrigin 제거 - Gateway에서 CORS 처리
public class BroadcastViewerController {

    private final BroadcastViewerService broadcastViewerService;

    /**
     * 방송 상세 정보 조회
     */
    @GetMapping("/{broadcastId}")
    public ResponseEntity<BroadcastDto.ViewerResponse> getBroadcastDetail(@PathVariable Long broadcastId) {
        try {
            BroadcastDto.ViewerResponse broadcast = broadcastViewerService.getBroadcastDetail(broadcastId);
            return ResponseEntity.ok(broadcast);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("방송 상세 조회 실패 - broadcastId: {}", broadcastId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 방송의 상품 목록 조회
     */
    @GetMapping("/{broadcastId}/products")
    public ResponseEntity<List<ProductDto.BroadcastProduct>> getBroadcastProducts(@PathVariable Long broadcastId) {
        try {
            List<ProductDto.BroadcastProduct> products = broadcastViewerService.getBroadcastProducts(broadcastId);
            return ResponseEntity.ok(products);
        } catch (Exception e) {
            log.error("방송 상품 목록 조회 실패 - broadcastId: {}", broadcastId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 방송 시청자 수 증가
     */
    @PostMapping("/{broadcastId}/view")
    public ResponseEntity<Map<String, Object>> increaseViewerCount(@PathVariable Long broadcastId) {
        try {
            Map<String, Object> result = broadcastViewerService.increaseViewerCount(broadcastId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("시청자 수 증가 실패 - broadcastId: {}", broadcastId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 방송 좋아요
     */
    @PostMapping("/{broadcastId}/like")
    public ResponseEntity<Map<String, Object>> likeBroadcast(@PathVariable Long broadcastId) {
        try {
            Map<String, Object> result = broadcastViewerService.likeBroadcast(broadcastId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("방송 좋아요 실패 - broadcastId: {}", broadcastId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 방송 상태 확인 (실시간 업데이트용)
     */
    @GetMapping("/{broadcastId}/status")
    public ResponseEntity<Map<String, Object>> getBroadcastStatus(@PathVariable Long broadcastId) {
        try {
            Map<String, Object> status = broadcastViewerService.getBroadcastStatus(broadcastId);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("방송 상태 확인 실패 - broadcastId: {}", broadcastId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 채팅 메시지 조회 (페이징)
     */
    @GetMapping("/{broadcastId}/chat")
    public ResponseEntity<List<Map<String, Object>>> getChatMessages(
            @PathVariable Long broadcastId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        try {
            List<Map<String, Object>> messages = broadcastViewerService.getChatMessages(broadcastId, page, size);
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            log.error("채팅 메시지 조회 실패 - broadcastId: {}", broadcastId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 채팅 메시지 전송
     */
    @PostMapping("/{broadcastId}/chat")
    public ResponseEntity<Map<String, Object>> sendChatMessage(
            @PathVariable Long broadcastId,
            @RequestBody Map<String, Object> messageData) {
        try {
            Map<String, Object> result = broadcastViewerService.sendChatMessage(broadcastId, messageData);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("채팅 메시지 전송 실패 - broadcastId: {}", broadcastId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}