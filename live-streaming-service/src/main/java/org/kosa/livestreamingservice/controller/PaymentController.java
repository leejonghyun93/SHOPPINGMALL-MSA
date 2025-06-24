package org.kosa.livestreamingservice.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kosa.livestreamingservice.dto.*;
import org.kosa.paymentservice.client.OrderServiceClient;
import org.kosa.paymentservice.service.PaymentService;
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
    private final OrderServiceClient orderServiceClient;

    /**
     *  가장 중요한 API: 결제 검증
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

            return ResponseEntity.ok(ApiResponse.success(response, "결제 검증이 완료되었습니다."));

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

            return ResponseEntity.ok(ApiResponse.success(response, "결제 준비가 완료되었습니다."));

        } catch (Exception e) {
            log.error("결제 준비 실패 - orderId: {}, error: {}", request.getOrderId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 결제 취소
     */
    @PostMapping("/{paymentId}/cancel")
    public ResponseEntity<ApiResponse<PaymentCancelResponse>> cancelPayment(
            @PathVariable String paymentId,
            @Valid @RequestBody PaymentCancelRequest request,
            Authentication authentication) {
        try {
            String userId = authentication != null ? authentication.getName() : "guest";

            log.info("결제 취소 요청 - paymentId: {}, userId: {}", paymentId, userId);

            PaymentCancelResponse response = paymentService.cancelPayment(paymentId, request, userId);

            return ResponseEntity.ok(ApiResponse.success(response, "결제가 취소되었습니다."));

        } catch (Exception e) {
            log.error("결제 취소 실패 - paymentId: {}, error: {}", paymentId, e.getMessage());
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

            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (Exception e) {
            log.error("결제 상태 조회 실패 - paymentId: {}, error: {}", paymentId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     *  웹훅 처리 (아임포트에서 호출 - 인증 제외)
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
    /**
     * Order Service 프록시 - 주문 생성
     */
    @PostMapping("/orders/checkout")
    public ResponseEntity<?> proxyCreateOrder(
            @RequestBody Map<String, Object> orderData,
            HttpServletRequest request) {
        try {
            String clientIp = getClientIp(request);
            log.info("주문 생성 프록시 요청 - clientIp: {}", clientIp);
            log.info("주문 데이터: {}", orderData);

            // Order Service로 요청 전달
            ResponseEntity<Map<String, Object>> response = orderServiceClient.createOrder(orderData);

            log.info("Order Service 응답 상태: {}", response.getStatusCode());
            log.info("Order Service 응답 데이터: {}", response.getBody());

            return response;

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
     * Order Service 프록시 - 주문 조회
     */
    @GetMapping("/orders/{orderId}")
    public ResponseEntity<?> proxyGetOrder(
            @PathVariable String orderId,
            @RequestParam(required = false) String userId,
            HttpServletRequest request) {
        try {
            String clientIp = getClientIp(request);
            log.info("주문 조회 프록시 요청 - orderId: {}, userId: {}, clientIp: {}", orderId, userId, clientIp);

            // Order Service로 요청 전달
            ResponseEntity<Map<String, Object>> response = orderServiceClient.getOrderById(orderId, userId);

            log.info("Order Service 응답 상태: {}", response.getStatusCode());
            log.info("Order Service 응답 데이터: {}", response.getBody());

            return response;

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

}