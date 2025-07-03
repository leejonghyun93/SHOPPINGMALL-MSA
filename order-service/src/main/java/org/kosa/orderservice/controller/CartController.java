package org.kosa.orderservice.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kosa.orderservice.dto.*;
import org.kosa.orderservice.service.CartService;
import org.kosa.orderservice.util.JwtTokenParser;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Slf4j
public class CartController {

    private final CartService cartService;
    private final JwtTokenParser jwtTokenParser;

    @PostMapping
    public ResponseEntity<ApiResponse<CartItemDTO>> addToCart(
            @RequestBody CartRequestDTO request,
            HttpServletRequest httpRequest) {

        try {
            // 🔥 순수 JWT 방식: Authorization 헤더에서만 사용자 ID 추출
            String authHeader = httpRequest.getHeader("Authorization");
            String userId = jwtTokenParser.extractUserIdFromAuthHeader(authHeader);

            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.<CartItemDTO>builder()
                                .success(false)
                                .message("로그인이 필요합니다.")
                                .build());
            }

            log.info("장바구니 추가 - userId: {}, productId: {}", userId, request.getProductId());

            CartItemDTO result = cartService.addToCart(userId, request);

            return ResponseEntity.ok(ApiResponse.<CartItemDTO>builder()
                    .success(true)
                    .message("장바구니에 상품이 추가되었습니다.")
                    .data(result)
                    .build());

        } catch (Exception e) {
            log.error("장바구니 추가 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<CartItemDTO>builder()
                            .success(false)
                            .message(e.getMessage() != null ? e.getMessage() : "장바구니 추가 중 오류가 발생했습니다.")
                            .build());
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<CartDTO>> getCart(HttpServletRequest httpRequest) {
        try {
            String authHeader = httpRequest.getHeader("Authorization");
            String userId = jwtTokenParser.extractUserIdFromAuthHeader(authHeader);

            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.<CartDTO>builder()
                                .success(false)
                                .message("로그인이 필요합니다.")
                                .build());
            }

            CartDTO cart = cartService.getCart(userId);

            return ResponseEntity.ok(ApiResponse.<CartDTO>builder()
                    .success(true)
                    .message("장바구니 조회 성공")
                    .data(cart)
                    .build());

        } catch (Exception e) {
            log.error("장바구니 조회 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<CartDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    @PutMapping("/items")
    public ResponseEntity<ApiResponse<CartItemDTO>> updateCartItemQuantity(
            @RequestBody CartUpdateRequestDTO request,
            HttpServletRequest httpRequest) {
        try {
            String authHeader = httpRequest.getHeader("Authorization");
            String userId = jwtTokenParser.extractUserIdFromAuthHeader(authHeader);

            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.<CartItemDTO>builder()
                                .success(false)
                                .message("로그인이 필요합니다.")
                                .build());
            }

            CartItemDTO result = cartService.updateCartItemQuantity(userId, request);
            String message = result != null ? "수량이 변경되었습니다." : "상품이 삭제되었습니다.";

            return ResponseEntity.ok(ApiResponse.<CartItemDTO>builder()
                    .success(true)
                    .message(message)
                    .data(result)
                    .build());

        } catch (Exception e) {
            log.error("장바구니 수량 변경 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<CartItemDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<ApiResponse<Void>> removeCartItem(
            @PathVariable String cartItemId,
            HttpServletRequest httpRequest) {

        try {
            String authHeader = httpRequest.getHeader("Authorization");
            String userId = jwtTokenParser.extractUserIdFromAuthHeader(authHeader);

            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.<Void>builder()
                                .success(false)
                                .message("로그인이 필요합니다.")
                                .build());
            }

            cartService.removeCartItem(userId, cartItemId);

            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(true)
                    .message("상품이 삭제되었습니다.")
                    .build());

        } catch (Exception e) {
            log.error("장바구니 상품 삭제 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<Void>builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    @DeleteMapping("/items")
    public ResponseEntity<ApiResponse<Void>> removeCartItems(
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {
        try {
            String authHeader = httpRequest.getHeader("Authorization");
            String userId = jwtTokenParser.extractUserIdFromAuthHeader(authHeader);

            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.<Void>builder()
                                .success(false)
                                .message("로그인이 필요합니다.")
                                .build());
            }

            @SuppressWarnings("unchecked")
            List<String> cartItemIds = (List<String>) request.get("cartItemIds");

            cartService.removeCartItems(userId, cartItemIds);

            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(true)
                    .message(cartItemIds.size() + "개 상품이 삭제되었습니다.")
                    .build());

        } catch (Exception e) {
            log.error("장바구니 상품 일괄 삭제 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<Void>builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> clearCart(HttpServletRequest httpRequest) {
        try {
            String authHeader = httpRequest.getHeader("Authorization");
            String userId = jwtTokenParser.extractUserIdFromAuthHeader(authHeader);

            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.<Void>builder()
                                .success(false)
                                .message("로그인이 필요합니다.")
                                .build());
            }

            cartService.clearCart(userId);

            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(true)
                    .message("장바구니가 비워졌습니다.")
                    .build());

        } catch (Exception e) {
            log.error("장바구니 전체 비우기 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<Void>builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Integer>> getCartItemCount(HttpServletRequest httpRequest) {
        try {
            String authHeader = httpRequest.getHeader("Authorization");
            String userId = jwtTokenParser.extractUserIdFromAuthHeader(authHeader);

            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.<Integer>builder()
                                .success(false)
                                .message("로그인이 필요합니다.")
                                .data(0)
                                .build());
            }

            CartDTO cart = cartService.getCart(userId);
            Integer count = cart.getTotalItems();

            return ResponseEntity.ok(ApiResponse.<Integer>builder()
                    .success(true)
                    .message("장바구니 상품 개수 조회 성공")
                    .data(count)
                    .build());

        } catch (Exception e) {
            log.error("장바구니 상품 개수 조회 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<Integer>builder()
                            .success(false)
                            .message(e.getMessage())
                            .data(0)
                            .build());
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Cart Service is running!");
    }

    @PostMapping("/remove-purchased-items")
    public ResponseEntity<?> removePurchasedItems(
            @RequestBody Map<String, List<Long>> request,
            HttpServletRequest httpRequest) {
        try {
            List<Long> productIds = request.get("productIds");

            if (productIds == null || productIds.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "제거할 상품 ID가 없습니다"));
            }

            String authHeader = httpRequest.getHeader("Authorization");
            String userId = jwtTokenParser.extractUserIdFromAuthHeader(authHeader);

            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "사용자 인증이 필요합니다"));
            }

            cartService.removePurchasedItems(userId, productIds);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "구매 상품이 장바구니에서 제거되었습니다",
                    "removedCount", productIds.size()
            ));

        } catch (Exception e) {
            log.error("구매 완료 상품 장바구니 제거 실패: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "장바구니 정리 중 오류가 발생했습니다: " + e.getMessage()
                    ));
        }
    }
}