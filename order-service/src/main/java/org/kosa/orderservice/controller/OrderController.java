// OrderController.java - URL 매핑 순서 수정
package org.kosa.orderservice.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kosa.orderservice.dto.*;
import org.kosa.orderservice.service.OrderService;
import org.kosa.orderservice.util.JwtTokenParser;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
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
    private final JwtTokenParser jwtTokenParser;

    /**
     * 주문 개수 조회
     */
    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOrderCount(
            HttpServletRequest httpRequest) {
        try {
            String authHeader = httpRequest.getHeader("Authorization");
            String userId = jwtTokenParser.extractUserIdFromAuthHeader(authHeader);

            if (userId == null) {
                Map<String, Object> result = new HashMap<>();
                result.put("count", 0);
                return ResponseEntity.ok(ApiResponse.success("주문 개수 조회 (비로그인)", result));
            }

            log.info("주문 개수 조회: userId={}", userId);

            List<OrderDTO> orders = orderService.getUserOrders(userId);
            int count = orders.size();

            Map<String, Object> result = new HashMap<>();
            result.put("count", count);
            result.put("userId", userId);

            return ResponseEntity.ok(ApiResponse.success("주문 개수 조회 성공", result));

        } catch (Exception e) {
            log.error("주문 개수 조회 실패: {}", e.getMessage(), e);

            Map<String, Object> result = new HashMap<>();
            result.put("count", 0);

            return ResponseEntity.ok(ApiResponse.success("주문 개수 조회 (기본값)", result));
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
     * 사용자 취소 주문 목록 조회
     */
    @GetMapping("/cancelled")
    public ResponseEntity<ApiResponse<List<OrderCancelResponseDTO>>> getCancelledOrders(
            HttpServletRequest httpRequest) {

        try {
            String authHeader = httpRequest.getHeader("Authorization");
            String userId = jwtTokenParser.extractUserIdFromAuthHeader(authHeader);

            if (userId == null) {
                return ResponseEntity.status(401)
                        .body(ApiResponse.error("로그인이 필요합니다."));
            }

            List<OrderCancelResponseDTO> cancelledOrders = orderService.getUserCancelledOrders(userId);
            return ResponseEntity.ok(ApiResponse.success(cancelledOrders));

        } catch (Exception e) {
            log.error("취소 주문 목록 조회 실패", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("취소 주문 목록 조회 중 오류가 발생했습니다."));
        }
    }

    /**
     * 🔥 사용자별 주문 목록 조회
     */
    @GetMapping("/list")
    public ResponseEntity<ApiResponse<List<OrderDTO>>> getOrderList(
            HttpServletRequest httpRequest) {
        try {
            String authHeader = httpRequest.getHeader("Authorization");
            String userId = jwtTokenParser.extractUserIdFromAuthHeader(authHeader);

            if (userId == null) {
                return ResponseEntity.status(401)
                        .body(ApiResponse.error("로그인이 필요합니다."));
            }

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
     * 🔥 주문 목록 페이징 조회
     */
    @GetMapping("/list/paged")
    public ResponseEntity<ApiResponse<Page<OrderDTO>>> getOrderListPaged(
            HttpServletRequest httpRequest,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        try {
            String authHeader = httpRequest.getHeader("Authorization");
            String userId = jwtTokenParser.extractUserIdFromAuthHeader(authHeader);

            if (userId == null) {
                return ResponseEntity.status(401)
                        .body(ApiResponse.error("로그인이 필요합니다."));
            }

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
     * 🔥 디버그용 엔드포인트들
     */
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
                List<String> allIds = orderService.getAllOrderIds();
                List<String> similarIds = allIds.stream()
                        .filter(id -> id.contains(orderId.substring(5, 15)))
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

    // ==================== {orderId}를 사용하는 매핑들은 아래쪽에 배치 ====================

    /**
     * 체크아웃 - 주문 생성
     */
    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> checkout(
            @RequestBody CheckoutRequestDTO request,
            HttpServletRequest httpRequest) {
        try {
            String authHeader = httpRequest.getHeader("Authorization");
            String userId = jwtTokenParser.extractUserIdFromAuthHeader(authHeader);

            // 게스트 처리
            if (userId == null) {
                userId = "guest_" + System.currentTimeMillis();
                log.info("게스트 사용자 체크아웃: userId={}", userId);
            } else {
                log.info("인증된 사용자 체크아웃: userId={}", userId);
            }

            request.setUserId(userId);

            OrderResponseDTO result = orderService.createOrder(request);

            return ResponseEntity.ok(ApiResponse.success("주문이 성공적으로 완료되었습니다.", result));

        } catch (Exception e) {
            log.error("체크아웃 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("주문 처리 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * 🔥 주문 상세 조회
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderDTO>> getOrderDetail(
            @PathVariable String orderId,
            HttpServletRequest httpRequest) {
        try {
            log.info("주문 상세 조회: orderId={}", orderId);

            String authHeader = httpRequest.getHeader("Authorization");
            String userId = jwtTokenParser.extractUserIdFromAuthHeader(authHeader);

            OrderDTO order;

            if (userId != null) {
                log.info("사용자별 주문 상세 조회: orderId={}, userId={}", orderId, userId);
                order = orderService.getOrderDetail(orderId, userId);
            } else {
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
     * 주문 취소 정보 조회
     */
    @GetMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponse<OrderCancelResponseDTO>> getCancelInfo(
            @PathVariable String orderId,
            HttpServletRequest httpRequest) {

        try {
            String authHeader = httpRequest.getHeader("Authorization");
            String userId = jwtTokenParser.extractUserIdFromAuthHeader(authHeader);

            if (userId == null) {
                return ResponseEntity.status(401)
                        .body(ApiResponse.error("로그인이 필요합니다."));
            }

            OrderCancelResponseDTO response = orderService.getCancelInfo(orderId, userId);
            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));

        } catch (Exception e) {
            log.error("주문 취소 정보 조회 실패", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("주문 취소 정보 조회 중 오류가 발생했습니다."));
        }
    }

    /**
     * 주문 취소
     */
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponse<OrderCancelResponseDTO>> cancelOrder(
            @PathVariable String orderId,
            @Valid @RequestBody OrderCancelRequestDTO request,
            HttpServletRequest httpRequest) {

        try {
            log.info("주문 취소 요청 - orderId: {}, userId: {}", orderId, request.getUserId());

            String authHeader = httpRequest.getHeader("Authorization");
            String authenticatedUserId = jwtTokenParser.extractUserIdFromAuthHeader(authHeader);

            if (authenticatedUserId == null) {
                log.warn("인증되지 않은 주문 취소 시도: orderId={}", orderId);
                return ResponseEntity.status(401)
                        .body(ApiResponse.error("인증이 필요합니다. 로그인 후 다시 시도해주세요."));
            }

            if (!authenticatedUserId.equals(request.getUserId())) {
                log.warn("권한 없는 주문 취소 시도: orderId={}, 인증된사용자={}, 요청사용자={}",
                        orderId, authenticatedUserId, request.getUserId());
                return ResponseEntity.status(403)
                        .body(ApiResponse.error("본인의 주문만 취소할 수 있습니다."));
            }

            if (!orderId.equals(request.getOrderId())) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("주문번호가 일치하지 않습니다."));
            }

            OrderCancelResponseDTO response = orderService.cancelOrder(request);

            log.info("주문 취소 성공: orderId={}", orderId);

            return ResponseEntity.ok(ApiResponse.success("주문이 성공적으로 취소되었습니다.", response));

        } catch (IllegalArgumentException e) {
            log.warn("주문 취소 요청 오류: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));

        } catch (IllegalStateException e) {
            log.warn("주문 취소 상태 오류: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));

        } catch (Exception e) {
            log.error("주문 취소 처리 실패", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("주문 취소 처리 중 오류가 발생했습니다."));
        }
    }

    /**
     * 주문 상태 변경 (관리자용)
     */
    @PutMapping("/{orderId}/status")
    public ResponseEntity<ApiResponse<Void>> updateOrderStatus(
            @PathVariable String orderId,
            @RequestParam String status,
            HttpServletRequest httpRequest) {
        try {
            String authHeader = httpRequest.getHeader("Authorization");
            String userId = jwtTokenParser.extractUserIdFromAuthHeader(authHeader);

            if (userId == null) {
                return ResponseEntity.status(401)
                        .body(ApiResponse.error("로그인이 필요합니다."));
            }

            log.info("주문 상태 변경: orderId={}, status={}, userId={}", orderId, status, userId);

            orderService.updateOrderStatus(orderId, status);

            return ResponseEntity.ok(ApiResponse.success("주문 상태가 변경되었습니다.", null));

        } catch (Exception e) {
            log.error("주문 상태 변경 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("주문 상태 변경 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @GetMapping("/{orderId}/cancelable")
    public ResponseEntity<ApiResponse<Boolean>> checkCancelable(
            @PathVariable String orderId,
            HttpServletRequest httpRequest) {

        try {
            String authHeader = httpRequest.getHeader("Authorization");
            String userId = jwtTokenParser.extractUserIdFromAuthHeader(authHeader);

            if (userId == null) {
                return ResponseEntity.status(401)
                        .body(ApiResponse.error("로그인이 필요합니다."));
            }

            boolean canCancel = orderService.canCancelOrder(orderId, userId);
            return ResponseEntity.ok(ApiResponse.success(canCancel));

        } catch (Exception e) {
            log.error("취소 가능 여부 확인 실패", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("취소 가능 여부 확인 중 오류가 발생했습니다."));
        }
    }

    /**
     * 간단 주문 취소
     */
    @PutMapping("/{orderId}/simple-cancel")
    public ResponseEntity<ApiResponse<String>> simpleCancelOrder(
            @PathVariable String orderId,
            HttpServletRequest httpRequest) {

        try {
            String authHeader = httpRequest.getHeader("Authorization");
            String userId = jwtTokenParser.extractUserIdFromAuthHeader(authHeader);

            if (userId == null) {
                return ResponseEntity.status(401)
                        .body(ApiResponse.error("로그인이 필요합니다."));
            }

            orderService.cancelOrder(orderId, userId);
            return ResponseEntity.ok(ApiResponse.success("주문이 취소되었습니다."));

        } catch (Exception e) {
            log.error("간단 주문 취소 실패", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
}