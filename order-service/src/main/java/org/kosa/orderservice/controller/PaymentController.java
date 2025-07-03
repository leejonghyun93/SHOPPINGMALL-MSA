package org.kosa.orderservice.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kosa.orderservice.dto.*;
import org.kosa.orderservice.service.PaymentService;
import org.kosa.orderservice.service.OrderService;
import org.kosa.orderservice.util.JwtTokenParser;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;
    private final OrderService orderService;
    private final JwtTokenParser jwtTokenParser;

    /**
     * 가장 중요한 API: 결제 검증
     */
    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<PaymentVerifyResponse>> verifyPayment(
            @Valid @RequestBody PaymentVerifyRequest request,
            HttpServletRequest httpRequest) {
        try {
            String authHeader = httpRequest.getHeader("Authorization");
            String userId = jwtTokenParser.extractUserIdFromAuthHeader(authHeader);

            // 게스트도 결제 가능
            if (userId == null) {
                userId = "guest_" + System.currentTimeMillis();
            }

            String clientIp = getClientIp(httpRequest);

            log.info("결제 검증 요청 - userId: {}, impUid: {}, clientIp: {}",
                    userId, request.getImpUid(), clientIp);

            PaymentVerifyResponse response = paymentService.verifyPayment(request, userId);

            return ResponseEntity.ok(ApiResponse.success("결제 검증이 완료되었습니다.", response));

        } catch (Exception e) {
            log.error("결제 검증 실패 - impUid: {}, error: {}", request.getImpUid(), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 결제 준비 (옵션)
     */
    @PostMapping("/prepare")
    public ResponseEntity<ApiResponse<PaymentPrepareResponse>> preparePayment(
            @Valid @RequestBody PaymentPrepareRequest request,
            HttpServletRequest httpRequest) {
        try {
            String authHeader = httpRequest.getHeader("Authorization");
            String userId = jwtTokenParser.extractUserIdFromAuthHeader(authHeader);

            if (userId == null) {
                userId = "guest_" + System.currentTimeMillis();
            }

            log.info("결제 준비 요청 - userId: {}, orderId: {}", userId, request.getOrderId());

            PaymentPrepareResponse response = paymentService.preparePayment(request);

            return ResponseEntity.ok(ApiResponse.success("결제 준비가 완료되었습니다.", response));

        } catch (Exception e) {
            log.error("결제 준비 실패 - orderId: {}, error: {}", request.getOrderId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 결제 취소
     */
    @PostMapping("/cancel")
    public ResponseEntity<ApiResponse<PaymentCancelResponseDTO>> cancelPayment(
            @Valid @RequestBody PaymentCancelRequestDTO request,
            HttpServletRequest httpRequest) {
        try {
            String authHeader = httpRequest.getHeader("Authorization");
            String userId = jwtTokenParser.extractUserIdFromAuthHeader(authHeader);

            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("로그인이 필요합니다."));
            }

            log.info("결제 취소 요청 - paymentId: {}, userId: {}", request.getPaymentId(), userId);

            PaymentCancelResponseDTO response = paymentService.cancelPayment(request);

            return ResponseEntity.ok(ApiResponse.success("결제가 취소되었습니다.", response));

        } catch (Exception e) {
            log.error("결제 취소 실패 - paymentId: {}, error: {}", request.getPaymentId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 결제 상태 조회
     */
    @GetMapping("/{paymentId}")
    public ResponseEntity<ApiResponse<PaymentStatusResponse>> getPaymentStatus(
            @PathVariable String paymentId,
            HttpServletRequest httpRequest) {
        try {
            String authHeader = httpRequest.getHeader("Authorization");
            String userId = jwtTokenParser.extractUserIdFromAuthHeader(authHeader);

            if (userId == null) {
                userId = "guest";
            }

            PaymentStatusResponse response = paymentService.getPaymentStatus(paymentId, userId);

            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (Exception e) {
            log.error("결제 상태 조회 실패 - paymentId: {}, error: {}", paymentId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 웹훅 처리 (아임포트에서 호출 - 인증 제외)
     */
    @PostMapping("/webhook")
    public ResponseEntity<Map<String, Object>> handleWebhook(
            @RequestBody Map<String, Object> webhookData,
            HttpServletRequest request) {
        try {
            String impUid = (String) webhookData.get("imp_uid");
            String merchantUid = (String) webhookData.get("merchant_uid");
            String clientIp = getClientIp(request);

            log.info("웹훅 수신 - impUid: {}, merchantUid: {}, clientIp: {}",
                    impUid, merchantUid, clientIp);

            paymentService.handleWebhook(impUid, merchantUid);

            return ResponseEntity.ok(Map.of("success", true));

        } catch (Exception e) {
            log.error("웹훅 처리 실패 - error: {}", e.getMessage());
            return ResponseEntity.ok(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Order Service 프록시 - 주문 생성
     */
    @PostMapping("/orders/checkout")
    public ResponseEntity<?> proxyCreateOrder(
            @RequestBody CheckoutRequestDTO orderData,
            HttpServletRequest httpRequest) {
        try {
            String authHeader = httpRequest.getHeader("Authorization");
            String userId = jwtTokenParser.extractUserIdFromAuthHeader(authHeader);

            if (userId == null) {
                userId = "guest_" + System.currentTimeMillis();
            }

            orderData.setUserId(userId);
            String clientIp = getClientIp(httpRequest);

            log.info("주문 생성 프록시 요청 - userId: {}, clientIp: {}", userId, clientIp);

            OrderResponseDTO response = orderService.createOrder(orderData);

            Map<String, Object> responseMap = Map.of(
                    "success", true,
                    "message", response.getMessage(),
                    "data", response
            );

            return ResponseEntity.ok(responseMap);

        } catch (Exception e) {
            log.error("주문 생성 프록시 실패: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = Map.of(
                    "success", false,
                    "message", "주문 생성 실패: " + e.getMessage(),
                    "error", e.getClass().getSimpleName()
            );

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse);
        }
    }

    /**
     * Order Service 프록시 - 주문 조회
     */
    @GetMapping("/orders/{orderId}")
    public ResponseEntity<?> proxyGetOrder(
            @PathVariable String orderId,
            HttpServletRequest httpRequest) {
        try {
            String authHeader = httpRequest.getHeader("Authorization");
            String userId = jwtTokenParser.extractUserIdFromAuthHeader(authHeader);
            String clientIp = getClientIp(httpRequest);

            log.info("주문 조회 프록시 요청 - orderId: {}, userId: {}, clientIp: {}", orderId, userId, clientIp);

            OrderDTO orderDTO;
            if (userId != null) {
                orderDTO = orderService.getOrderDetail(orderId, userId);
            } else {
                orderDTO = orderService.getOrderDetailByOrderId(orderId);
            }

            Map<String, Object> responseMap = Map.of(
                    "success", true,
                    "message", "주문 조회 성공",
                    "data", orderDTO
            );

            return ResponseEntity.ok(responseMap);

        } catch (Exception e) {
            log.error("주문 조회 프록시 실패 - orderId: {}, error: {}", orderId, e.getMessage(), e);

            Map<String, Object> errorResponse = Map.of(
                    "success", false,
                    "message", "주문 조회 실패: " + e.getMessage(),
                    "error", e.getClass().getSimpleName()
            );

            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(errorResponse);
        }
    }

    // === 유틸리티 메서드들 ===

    /**
     * 🔥 순수 방식: X-*** 헤더 사용하지 않고 직접 IP 추출
     */
    private String getClientIp(HttpServletRequest request) {
        // X-Forwarded-For, X-Real-IP 등 X-*** 헤더 사용하지 않음
        // 직접 클라이언트 IP만 사용
        String clientIp = request.getRemoteAddr();

        if (clientIp == null || clientIp.isEmpty()) {
            clientIp = "unknown";
        }

        log.debug("클라이언트 IP 추출: {}", clientIp);
        return clientIp;
    }
}