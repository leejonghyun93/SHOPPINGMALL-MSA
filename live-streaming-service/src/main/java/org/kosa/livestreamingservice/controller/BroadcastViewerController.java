package org.kosa.livestreamingservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kosa.livestreamingservice.dto.ViewerResponse;
import org.kosa.livestreamingservice.dto.ProductDto;
import org.kosa.livestreamingservice.service.BroadcastViewerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "방송 시청자 API", description = "방송 시청자용 API")
@RestController
@RequestMapping("/api/broadcast")
@RequiredArgsConstructor
@Slf4j
public class BroadcastViewerController {

    private final BroadcastViewerService broadcastViewerService;

    @Operation(summary = "방송 상세 정보 조회 (시청자용)", description = "시청자용 방송 상세 정보를 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = ViewerResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "방송을 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/{broadcastId}")
    public ResponseEntity<ViewerResponse> getBroadcastDetail(
            @Parameter(description = "방송 ID", required = true, example = "1")
            @PathVariable Long broadcastId) {
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

    @Operation(summary = "방송 상품 목록 조회", description = "특정 방송에서 판매하는 상품 목록을 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = ProductDto.BroadcastProduct.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/{broadcastId}/products")
    public ResponseEntity<List<ProductDto.BroadcastProduct>> getBroadcastProducts(
            @Parameter(description = "방송 ID", required = true, example = "1")
            @PathVariable Long broadcastId) {
        try {
            List<ProductDto.BroadcastProduct> products = broadcastViewerService.getBroadcastProducts(broadcastId);
            return ResponseEntity.ok(products);
        } catch (Exception e) {
            log.error("방송 상품 목록 조회 실패 - broadcastId: {}", broadcastId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "시청자 수 증가", description = "방송의 시청자 수를 증가시킵니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "증가 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PostMapping("/{broadcastId}/view")
    public ResponseEntity<Map<String, Object>> increaseViewerCount(
            @Parameter(description = "방송 ID", required = true, example = "1")
            @PathVariable Long broadcastId) {
        try {
            Map<String, Object> result = broadcastViewerService.increaseViewerCount(broadcastId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("시청자 수 증가 실패 - broadcastId: {}", broadcastId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "방송 좋아요", description = "방송에 좋아요를 추가합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "좋아요 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PostMapping("/{broadcastId}/like")
    public ResponseEntity<Map<String, Object>> likeBroadcast(
            @Parameter(description = "방송 ID", required = true, example = "1")
            @PathVariable Long broadcastId) {
        try {
            Map<String, Object> result = broadcastViewerService.likeBroadcast(broadcastId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("방송 좋아요 실패 - broadcastId: {}", broadcastId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "방송 상태 확인", description = "방송의 현재 상태를 확인합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/{broadcastId}/status")
    public ResponseEntity<Map<String, Object>> getBroadcastStatus(
            @Parameter(description = "방송 ID", required = true, example = "1")
            @PathVariable Long broadcastId) {
        try {
            Map<String, Object> status = broadcastViewerService.getBroadcastStatus(broadcastId);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("방송 상태 확인 실패 - broadcastId: {}", broadcastId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}