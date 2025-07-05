package org.kosa.commerceservice.controller.payment;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.kosa.commerceservice.dto.ApiResponse;
import org.kosa.commerceservice.dto.order.CheckoutRequestDTO;
import org.kosa.commerceservice.dto.order.OrderResponseDTO;
import org.kosa.commerceservice.dto.payment.*;
import org.kosa.commerceservice.entity.payment.Payment;
import org.kosa.commerceservice.service.order.OrderService;
import org.kosa.commerceservice.service.payment.PaymentService;
import org.kosa.commerceservice.util.JwtTokenParser;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;
    private final OrderService orderService;
    private final JwtTokenParser jwtTokenParser;

    /**
     * 프론트엔드에서 호출하는 주문 생성 API
     * POST /api/payments/orders/checkout
     */
    @PostMapping("/orders/checkout")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> createOrderForPayment(
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

            log.info("결제용 주문 생성 요청 - userId: {}, items: {}", userId, request.getItems().size());

            OrderResponseDTO result = orderService.createOrder(request);

            return ResponseEntity.ok(ApiResponse.success("주문이 성공적으로 생성되었습니다.", result));

        } catch (Exception e) {
            log.error("결제용 주문 생성 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("주문 생성 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * impUid로 결제 취소하는 API (프론트엔드 호환성)
     * POST /api/payments/{impUid}/cancel
     */
    @PostMapping("/{impUid}/cancel")
    public ResponseEntity<ApiResponse<PaymentCancelResponseDTO>> cancelPaymentByImpUid(
            @PathVariable String impUid,
            HttpServletRequest httpRequest) {
        try {
            String authHeader = httpRequest.getHeader("Authorization");
            String userId = jwtTokenParser.extractUserIdFromAuthHeader(authHeader);

            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("로그인이 필요합니다."));
            }

            log.info("impUid로 결제 취소 요청 - impUid: {}, userId: {}", impUid, userId);

            // PaymentService를 통해 impUid로 결제 취소 처리
            PaymentCancelResponseDTO response = paymentService.cancelPaymentByImpUid(impUid, userId, "사용자 요청에 의한 취소");

            return ResponseEntity.ok(ApiResponse.success("결제가 취소되었습니다.", response));

        } catch (Exception e) {
            log.error("결제 취소 실패 - impUid: {}, error: {}", impUid, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<PaymentVerifyResponse>> verifyPayment(
            @Valid @RequestBody PaymentVerifyRequest request,
            HttpServletRequest httpRequest) {
        try {
            String authHeader = httpRequest.getHeader("Authorization");
            String userId = jwtTokenParser.extractUserIdFromAuthHeader(authHeader);

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

    private String getClientIp(HttpServletRequest request) {
        String clientIp = request.getRemoteAddr();
        if (clientIp == null || clientIp.isEmpty()) {
            clientIp = "unknown";
        }
        log.debug("클라이언트 IP 추출: {}", clientIp);
        return clientIp;
    }
}