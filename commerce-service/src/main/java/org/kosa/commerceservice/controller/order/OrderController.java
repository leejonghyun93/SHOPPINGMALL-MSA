package org.kosa.commerceservice.controller.order;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.kosa.commerceservice.dto.ApiResponse;
import org.kosa.commerceservice.dto.order.*;
import org.kosa.commerceservice.service.order.OrderService;
import org.kosa.commerceservice.util.JwtTokenParser;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
@Slf4j
public class OrderController {

    private final OrderService orderService;
    private final JwtTokenParser jwtTokenParser;
    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Integer>> getOrderCount(
            HttpServletRequest httpRequest) {
        try {
            String authHeader = httpRequest.getHeader("Authorization");
            String userId = jwtTokenParser.extractUserIdFromAuthHeader(authHeader);

            if (userId == null) {
                return ResponseEntity.status(401)
                        .body(ApiResponse.error("로그인이 필요합니다."));
            }

            log.info("주문 개수 조회: userId={}", userId);

            int orderCount = orderService.getOrderCount(userId);

            return ResponseEntity.ok(ApiResponse.success("주문 개수 조회 성공", orderCount));

        } catch (Exception e) {
            log.error("주문 개수 조회 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("주문 개수 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> checkout(
            @RequestBody CheckoutRequestDTO request,
            HttpServletRequest httpRequest) {
        try {
            String authHeader = httpRequest.getHeader("Authorization");
            String userId = jwtTokenParser.extractUserIdFromAuthHeader(authHeader);

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
}