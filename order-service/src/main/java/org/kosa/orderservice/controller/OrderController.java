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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
            log.info("주문 상세 조회: orderId={}", orderId);

            // 🔧 수정: userId가 없어도 주문 조회 가능
            OrderDTO order;

            // userId가 제공된 경우 권한 검증과 함께 조회
            if (hasUserId(authentication, headerUserId, paramUserId)) {
                String userId = getUserId(authentication, headerUserId, paramUserId);
                log.info("사용자별 주문 상세 조회: orderId={}, userId={}", orderId, userId);
                order = orderService.getOrderDetail(orderId, userId);
            } else {
                // userId가 없는 경우 주문번호만으로 조회 (주문 완료 페이지용)
                log.info("공개 주문 상세 조회: orderId={}", orderId);
                order = orderService.getOrderDetailByOrderId(orderId);
            }

            return ResponseEntity.ok(ApiResponse.success("주문 상세 조회 성공", order));

        } catch (Exception e) {
            log.error("주문 상세 조회 실패: orderId={}, error={}", orderId, e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("주문 상세 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * userId가 제공되었는지 확인
     */
    private boolean hasUserId(Authentication authentication, String headerUserId, String paramUserId) {
        // JWT 인증된 사용자
        if (authentication != null && authentication.isAuthenticated() &&
                !"anonymousUser".equals(authentication.getName())) {
            return true;
        }

        // 헤더나 파라미터에 유효한 userId가 있는지
        return (headerUserId != null && !headerUserId.trim().isEmpty()) ||
                (paramUserId != null && !paramUserId.trim().isEmpty());
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
        // 🔧 수정: 요청 바디의 userId를 최우선으로 처리
        if (requestUserId != null && !requestUserId.trim().isEmpty() &&
                !"null".equals(requestUserId) && !requestUserId.startsWith("guest_")) {
            log.debug("요청 바디 사용자 ID 사용: {}", requestUserId);
            return requestUserId;
        }

        // 1. JWT 인증된 사용자 (두 번째 우선순위)
        if (authentication != null && authentication.isAuthenticated() &&
                !"anonymousUser".equals(authentication.getName())) {
            log.debug("JWT 인증 사용자: {}", authentication.getName());
            return authentication.getName();
        }

        // 2. 헤더의 사용자 ID
        if (headerUserId != null && !headerUserId.trim().isEmpty() &&
                !headerUserId.startsWith("guest_")) {
            log.debug("헤더 사용자 ID 사용: {}", headerUserId);
            return headerUserId;
        }

        // 3. 기존 게스트 ID 재사용
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
    @GetMapping("/debug/list")
    public ResponseEntity<?> debugOrderList() {
        try {
            List<String> allOrderIds = orderService.getAllOrderIds();

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("total", allOrderIds.size());
            result.put("orderIds", allOrderIds.stream().limit(10).collect(Collectors.toList()));
            result.put("recentOrders", allOrderIds.stream()
                    .filter(id -> id.contains("ORDER175"))
                    .limit(5)
                    .collect(Collectors.toList()));

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.ok(error);
        }
    }
    @GetMapping("/debug/exists/{orderId}")
    public ResponseEntity<?> debugOrderExists(@PathVariable String orderId) {
        try {
            boolean exists = orderService.orderExists(orderId);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("orderId", orderId);
            result.put("exists", exists);

            if (!exists) {
                // 비슷한 ID 검색
                List<String> allIds = orderService.getAllOrderIds();
                List<String> similarIds = allIds.stream()
                        .filter(id -> id.contains(orderId.substring(5, 15))) // ORDER 이후 10자리
                        .limit(3)
                        .collect(Collectors.toList());
                result.put("similarIds", similarIds);
            }

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.ok(error);
        }
    }

}



