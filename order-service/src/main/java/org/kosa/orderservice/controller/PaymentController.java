package org.kosa.orderservice.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kosa.orderservice.dto.*;
import org.kosa.orderservice.service.PaymentService;
import org.kosa.orderservice.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;
    private final OrderService orderService; // 🔥 내부 서비스로 변경

    /**
     * 가장 중요한 API: 결제 검증
     * 프론트엔드에서 아임포트 결제 완료 후 반드시 호출
     */
    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<PaymentVerifyResponse>> verifyPayment(
            @Valid @RequestBody PaymentVerifyRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        try {
            // 인증 정보가 없으면 게스트로 처리
            String userId = authentication != null ? authentication.getName() : "guest";
            String clientIp = getClientIp(httpRequest);

            log.info("결제 검증 요청 - userId: {}, impUid: {}, clientIp: {}",
                    userId, request.getImpUid(), clientIp);

            PaymentVerifyResponse response = paymentService.verifyPayment(request, userId);

            // 🔥 수정: success(message, data) 형태로 호출
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
            Authentication authentication) {
        try {
            String userId = authentication != null ? authentication.getName() : "guest";

            log.info("결제 준비 요청 - userId: {}, orderId: {}", userId, request.getOrderId());

            PaymentPrepareResponse response = paymentService.preparePayment(request);

            // 🔥 수정: success(message, data) 형태로 호출
            return ResponseEntity.ok(ApiResponse.success("결제 준비가 완료되었습니다.", response));

        } catch (Exception e) {
            log.error("결제 준비 실패 - orderId: {}, error: {}", request.getOrderId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 결제 취소 (🔥 Order Service에서 직접 호출하므로 내부 API)
     */
    @PostMapping("/cancel")
    public ResponseEntity<ApiResponse<PaymentCancelResponseDTO>> cancelPayment(
            @Valid @RequestBody PaymentCancelRequestDTO request,
            Authentication authentication) {
        try {
            String userId = authentication != null ? authentication.getName() : "guest";

            log.info("결제 취소 요청 - paymentId: {}, userId: {}", request.getPaymentId(), userId);

            PaymentCancelResponseDTO response = paymentService.cancelPayment(request);

            // 🔥 수정: success(message, data) 형태로 호출
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
            Authentication authentication) {
        try {
            String userId = authentication != null ? authentication.getName() : "guest";

            PaymentStatusResponse response = paymentService.getPaymentStatus(paymentId, userId);

            // 🔥 수정: success(data) 형태로 호출
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
     * 🔥 Order Service 프록시 - 주문 생성 (내부 서비스 호출로 변경)
     */
    @PostMapping("/orders/checkout")
    public ResponseEntity<?> proxyCreateOrder(
            @RequestBody CheckoutRequestDTO orderData,
            HttpServletRequest request) {
        try {
            String clientIp = getClientIp(request);
            log.info("주문 생성 프록시 요청 - clientIp: {}", clientIp);
            log.info("주문 데이터: {}", orderData);

            // 🔥 내부 OrderService로 직접 호출
            OrderResponseDTO response = orderService.createOrder(orderData);

            log.info("Order Service 응답: {}", response);

            // API 호환성을 위해 Map 형태로 응답
            Map<String, Object> responseMap = Map.of(
                    "success", true,
                    "message", response.getMessage(),
                    "data", response
            );

            return ResponseEntity.ok(responseMap);

        } catch (Exception e) {
            log.error("주문 생성 프록시 실패: {}", e.getMessage(), e);

            // 에러 응답 생성
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
     * 🔥 Order Service 프록시 - 주문 조회 (내부 서비스 호출로 변경)
     */
    @GetMapping("/orders/{orderId}")
    public ResponseEntity<?> proxyGetOrder(
            @PathVariable String orderId,
            @RequestParam(required = false) String userId,
            HttpServletRequest request) {
        try {
            String clientIp = getClientIp(request);
            log.info("주문 조회 프록시 요청 - orderId: {}, userId: {}, clientIp: {}", orderId, userId, clientIp);

            // 🔥 내부 OrderService로 직접 호출
            OrderDTO orderDTO;
            if (userId != null && !userId.trim().isEmpty()) {
                orderDTO = orderService.getOrderDetail(orderId, userId);
            } else {
                orderDTO = orderService.getOrderDetailByOrderId(orderId);
            }

            // API 호환성을 위해 Map 형태로 응답
            Map<String, Object> responseMap = Map.of(
                    "success", true,
                    "message", "주문 조회 성공",
                    "data", orderDTO
            );

            log.info("Order Service 응답: {}", responseMap);

            return ResponseEntity.ok(responseMap);

        } catch (Exception e) {
            log.error("주문 조회 프록시 실패 - orderId: {}, error: {}", orderId, e.getMessage(), e);

            // 에러 응답 생성
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

    private String getClientIp(HttpServletRequest request) {
        String clientIp = request.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isEmpty() || "unknown".equalsIgnoreCase(clientIp)) {
            clientIp = request.getHeader("X-Real-IP");
        }
        if (clientIp == null || clientIp.isEmpty() || "unknown".equalsIgnoreCase(clientIp)) {
            clientIp = request.getRemoteAddr();
        }
        if (clientIp != null && clientIp.contains(",")) {
            clientIp = clientIp.split(",")[0].trim();
        }
        return clientIp;
    }
}