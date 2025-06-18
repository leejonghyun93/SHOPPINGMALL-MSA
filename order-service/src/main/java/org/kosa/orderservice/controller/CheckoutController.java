package org.kosa.orderservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kosa.orderservice.dto.*;
import org.kosa.orderservice.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kosa.orderservice.dto.*;
import org.kosa.orderservice.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/checkout")
@Slf4j
public class CheckoutController {

    private final OrderService orderService;

    /**
     * 체크아웃 페이지 정보 조회 (장바구니 정보 기반)
     */
    @PostMapping("/prepare")
    public ResponseEntity<ApiResponse<CheckoutPrepareResponseDTO>> prepareCheckout(
            Authentication authentication,
            @RequestBody CheckoutPrepareRequestDTO request,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId) {
        try {
            String userId = getUserId(authentication, headerUserId, request.getUserId());
            log.info("체크아웃 준비: userId={}, itemCount={}", userId, request.getItems().size());

            // 총 상품 금액 계산
            Integer totalItemPrice = request.getItems().stream()
                    .mapToInt(item -> item.getTotalPrice())  // unitPrice * quantity 대신 totalPrice 사용
                    .sum();

            // 배송비 계산
            Integer deliveryFee = totalItemPrice >= 40000 ? 0 : 3000;

            // 할인 금액 계산 (임시로 0)
            Integer discountAmount = 0;

            // 최종 결제 금액
            Integer finalAmount = totalItemPrice + deliveryFee - discountAmount;

            // 적립 예정 포인트 (결제 금액의 1%)
            Integer expectedPoint = (int) (finalAmount * 0.01);

            CheckoutPrepareResponseDTO response = CheckoutPrepareResponseDTO.builder()
                    .userId(userId)
                    .items(request.getItems())
                    .totalItemPrice(totalItemPrice)
                    .deliveryFee(deliveryFee)
                    .discountAmount(discountAmount)
                    .finalAmount(finalAmount)
                    .expectedPoint(expectedPoint)
                    .build();

            return ResponseEntity.ok(ApiResponse.success("체크아웃 정보 조회 성공", response));

        } catch (Exception e) {
            log.error("체크아웃 준비 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("체크아웃 준비 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * 주문 완료 후 결과 조회
     */
    @GetMapping("/result/{orderId}")
    public ResponseEntity<ApiResponse<OrderDTO>> getCheckoutResult(
            @PathVariable String orderId,
            Authentication authentication,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @RequestParam(value = "userId", required = false) String paramUserId) {
        try {
            String userId = getUserId(authentication, headerUserId, paramUserId);
            log.info("주문 결과 조회: orderId={}, userId={}", orderId, userId);

            OrderDTO order = orderService.getOrderDetail(orderId, userId);

            return ResponseEntity.ok(ApiResponse.success("주문 결과 조회 성공", order));

        } catch (Exception e) {
            log.error("주문 결과 조회 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("주문 결과 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * 사용자 ID 결정 로직
     */
    private String getUserId(Authentication authentication, String headerUserId, String requestUserId) {
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }

        if (headerUserId != null && !headerUserId.trim().isEmpty()) {
            return headerUserId;
        }

        if (requestUserId != null && !requestUserId.trim().isEmpty()) {
            return requestUserId;
        }

        return "guest_" + System.currentTimeMillis();
    }
}