// OrderController.java
package org.kosa.orderservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kosa.orderservice.dto.*;
import org.kosa.orderservice.service.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
@Slf4j
public class OrderController {

    private final OrderService orderService;

    /**
     * 체크아웃 - 주문 생성
     */
    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> checkout(
            Authentication authentication,
            @RequestBody CheckoutRequestDTO request,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            // 사용자 ID 결정 (조건부 인증)
            String userId = getUserId(authentication, headerUserId, request.getUserId());
            request.setUserId(userId);

            // 인증 타입 로깅
            if (authentication != null && authentication.isAuthenticated()) {
                log.info("인증된 사용자 체크아웃: userId={}, itemCount={}", userId, request.getItems().size());
            } else if (headerUserId != null) {
                log.info("헤더 기반 사용자 체크아웃: userId={}, itemCount={}", userId, request.getItems().size());
            } else {
                log.info("게스트 사용자 체크아웃: userId={}, itemCount={}", userId, request.getItems().size());
            }

            OrderResponseDTO result = orderService.createOrder(request);

            return ResponseEntity.ok(ApiResponse.success("주문이 성공적으로 완료되었습니다.", result));

        } catch (Exception e) {
            log.error("체크아웃 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("주문 처리 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * 사용자별 주문 목록 조회
     */
    @GetMapping("/list")
    public ResponseEntity<ApiResponse<List<OrderDTO>>> getOrderList(
            Authentication authentication,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @RequestParam(value = "userId", required = false) String paramUserId) {
        try {
            String userId = getUserId(authentication, headerUserId, paramUserId);
            log.info("주문 목록 조회: userId={}", userId);

            List<OrderDTO> orders = orderService.getUserOrders(userId);

            return ResponseEntity.ok(ApiResponse.success("주문 목록 조회 성공", orders));

        } catch (Exception e) {
            log.error("주문 목록 조회 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("주문 목록 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * 주문 목록 페이징 조회
     */
    @GetMapping("/list/paged")
    public ResponseEntity<ApiResponse<Page<OrderDTO>>> getOrderListPaged(
            Authentication authentication,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @RequestParam(value = "userId", required = false) String paramUserId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        try {
            String userId = getUserId(authentication, headerUserId, paramUserId);
            log.info("주문 목록 페이징 조회: userId={}, page={}, size={}", userId, page, size);

            Page<OrderDTO> orderPage = orderService.getUserOrdersPaged(userId, page, size);

            return ResponseEntity.ok(ApiResponse.success("주문 목록 조회 성공", orderPage));

        } catch (Exception e) {
            log.error("주문 목록 페이징 조회 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("주문 목록 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * 주문 상세 조회
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderDTO>> getOrderDetail(
            @PathVariable String orderId,
            Authentication authentication,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @RequestParam(value = "userId", required = false) String paramUserId) {
        try {
            String userId = getUserId(authentication, headerUserId, paramUserId);
            log.info("주문 상세 조회: orderId={}, userId={}", orderId, userId);

            OrderDTO order = orderService.getOrderDetail(orderId, userId);

            return ResponseEntity.ok(ApiResponse.success("주문 상세 조회 성공", order));

        } catch (Exception e) {
            log.error("주문 상세 조회 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("주문 상세 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * 주문 취소
     */
    @PutMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelOrder(
            @PathVariable String orderId,
            Authentication authentication,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @RequestParam(value = "userId", required = false) String paramUserId) {
        try {
            String userId = getUserId(authentication, headerUserId, paramUserId);
            log.info("주문 취소: orderId={}, userId={}", orderId, userId);

            orderService.cancelOrder(orderId, userId);

            return ResponseEntity.ok(ApiResponse.success("주문이 취소되었습니다.", null));

        } catch (Exception e) {
            log.error("주문 취소 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("주문 취소 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * 주문 상태 변경 (관리자용)
     */
    @PutMapping("/{orderId}/status")
    public ResponseEntity<ApiResponse<Void>> updateOrderStatus(
            @PathVariable String orderId,
            @RequestParam String status,
            Authentication authentication) {
        try {
            log.info("주문 상태 변경: orderId={}, status={}", orderId, status);

            orderService.updateOrderStatus(orderId, status);

            return ResponseEntity.ok(ApiResponse.success("주문 상태가 변경되었습니다.", null));

        } catch (Exception e) {
            log.error("주문 상태 변경 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("주문 상태 변경 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * 헬스체크
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Order Service is running!");
    }

    /**
     * 사용자 ID 결정 우선순위:
     * 1. 인증된 사용자 (JWT 토큰)
     * 2. 헤더의 X-User-Id
     * 3. 요청 파라미터/바디의 userId
     * 4. 게스트 사용자 ID 생성
     */
    private String getUserId(Authentication authentication, String headerUserId, String requestUserId) {
        // 1. JWT 인증된 사용자 (최우선)
        if (authentication != null && authentication.isAuthenticated() &&
                !"anonymousUser".equals(authentication.getName())) {
            log.debug("JWT 인증 사용자: {}", authentication.getName());
            return authentication.getName();
        }

        // 2. 헤더의 사용자 ID (세션 등에서 관리되는 사용자)
        if (headerUserId != null && !headerUserId.trim().isEmpty() &&
                !headerUserId.startsWith("guest_")) {
            log.debug("헤더 사용자 ID 사용: {}", headerUserId);
            return headerUserId;
        }

        // 3. 요청의 사용자 ID (클라이언트에서 전달)
        if (requestUserId != null && !requestUserId.trim().isEmpty() &&
                !requestUserId.startsWith("guest_")) {
            log.debug("요청 사용자 ID 사용: {}", requestUserId);
            return requestUserId;
        }

        // 4. 기존 게스트 ID 재사용 (헤더나 요청에 게스트 ID가 있는 경우)
        if (headerUserId != null && headerUserId.startsWith("guest_")) {
            log.debug("기존 게스트 ID 재사용: {}", headerUserId);
            return headerUserId;
        }

        if (requestUserId != null && requestUserId.startsWith("guest_")) {
            log.debug("기존 게스트 ID 재사용: {}", requestUserId);
            return requestUserId;
        }
        String guestId = "guest_" + System.currentTimeMillis() + "_" + (int) (Math.random() * 1000);
        log.debug("새 게스트 ID 생성: {}", guestId);
        return guestId;
    }
}



