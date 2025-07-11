package org.kosa.livestreamingservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kosa.livestreamingservice.dto.ViewerResponse; // 🔥 별도 파일 import
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
public class BroadcastViewerController {

    private final BroadcastViewerService broadcastViewerService;

    /**
     * 방송 상세 정보 조회 -  ViewerResponse 사용
     */
    @GetMapping("/{broadcastId}")
    public ResponseEntity<ViewerResponse> getBroadcastDetail(@PathVariable Long broadcastId) {
        try {
            ViewerResponse broadcast = broadcastViewerService.getBroadcastDetail(broadcastId);
            return ResponseEntity.ok(broadcast);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("방송 상세 조회 실패 - broadcastId: {}", broadcastId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // 나머지 메소드들은 동일...
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
}