package org.kosa.commerceservice.controller.payment;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.kosa.commerceservice.dto.ApiResponse;
import org.kosa.commerceservice.dto.order.CheckoutRequestDTO;
import org.kosa.commerceservice.dto.order.OrderResponseDTO;
import org.kosa.commerceservice.dto.payment.*;
import org.kosa.commerceservice.service.order.OrderService;
import org.kosa.commerceservice.service.payment.PaymentService;
import org.kosa.commerceservice.util.JwtTokenParser;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "결제 API", description = "결제 관련 API")
public class PaymentController {

    private final PaymentService paymentService;
    private final OrderService orderService;
    private final JwtTokenParser jwtTokenParser;

    @PostMapping("/orders/checkout")
    @Operation(
            summary = "결제용 주문 생성",
            description = "프론트엔드에서 결제 전 주문을 생성합니다. 로그인하지 않은 경우 게스트 사용자로 처리됩니다."
    )
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

    @PostMapping("/{impUid}/cancel")
    @Operation(
            summary = "impUid 기반 결제 취소",
            description = "impUid를 사용하여 결제를 취소합니다. 로그인한 사용자만 요청할 수 있습니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<PaymentCancelResponseDTO>> cancelPaymentByImpUid(
            @Parameter(description = "아임포트 imp_uid", example = "imp_123456789") @PathVariable String impUid,
            HttpServletRequest httpRequest) {
        try {
            String authHeader = httpRequest.getHeader("Authorization");
            String userId = jwtTokenParser.extractUserIdFromAuthHeader(authHeader);

            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("로그인이 필요합니다."));
            }

            log.info("impUid로 결제 취소 요청 - impUid: {}, userId: {}", impUid, userId);
            PaymentCancelResponseDTO response = paymentService.cancelPaymentByImpUid(impUid, userId, "사용자 요청에 의한 취소");

            return ResponseEntity.ok(ApiResponse.success("결제가 취소되었습니다.", response));

        } catch (Exception e) {
            log.error("결제 취소 실패 - impUid: {}, error: {}", impUid, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/verify")
    @Operation(
            summary = "결제 검증",
            description = "impUid를 기반으로 결제 결과를 검증합니다."
    )
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
            log.info("결제 검증 요청 - userId: {}, impUid: {}, clientIp: {}", userId, request.getImpUid(), clientIp);

            PaymentVerifyResponse response = paymentService.verifyPayment(request, userId);
            return ResponseEntity.ok(ApiResponse.success("결제 검증이 완료되었습니다.", response));

        } catch (Exception e) {
            log.error("결제 검증 실패 - impUid: {}, error: {}", request.getImpUid(), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/prepare")
    @Operation(
            summary = "결제 준비",
            description = "결제 전 아임포트와 금액 사전 등록을 처리합니다."
    )
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
    @Operation(
            summary = "결제 취소",
            description = "paymentId를 이용한 일반 결제 취소 처리입니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
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
    @Operation(
            summary = "아임포트 웹훅 수신",
            description = "아임포트에서 전송하는 웹훅 이벤트를 수신하여 결제 상태를 업데이트합니다."
    )
    public ResponseEntity<Map<String, Object>> handleWebhook(
            @RequestBody Map<String, Object> webhookData,
            HttpServletRequest request) {
        try {
            String impUid = (String) webhookData.get("imp_uid");
            String merchantUid = (String) webhookData.get("merchant_uid");
            String clientIp = getClientIp(request);

            log.info("웹훅 수신 - impUid: {}, merchantUid: {}, clientIp: {}", impUid, merchantUid, clientIp);
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
