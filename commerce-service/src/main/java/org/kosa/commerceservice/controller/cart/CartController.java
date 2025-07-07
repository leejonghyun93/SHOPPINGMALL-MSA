package org.kosa.commerceservice.controller.cart;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.kosa.commerceservice.dto.ApiResponse;
import org.kosa.commerceservice.dto.cart.CartDTO;
import org.kosa.commerceservice.dto.cart.CartItemDTO;
import org.kosa.commerceservice.dto.cart.CartRequestDTO;
import org.kosa.commerceservice.dto.cart.CartUpdateRequestDTO;
import org.kosa.commerceservice.service.cart.CartService;
import org.kosa.commerceservice.util.JwtTokenParser;
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
    public ResponseEntity<ApiResponse<Void>> updateCartItem(
            @RequestBody CartUpdateRequestDTO request,
            HttpServletRequest httpRequest) {

        try {
            log.info("🔄 장바구니 수량 변경 요청 - cartItemId: {}, quantity: {}",
                    request.getCartItemId(), request.getQuantity());

            String authHeader = httpRequest.getHeader("Authorization");
            String userId = jwtTokenParser.extractUserIdFromAuthHeader(authHeader);

            if (userId == null) {
                log.warn("⚠️ 인증 실패 - Authorization 헤더: {}", authHeader);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.<Void>builder()
                                .success(false)
                                .message("로그인이 필요합니다.")
                                .build());
            }

            log.info("✅ 인증 성공 - userId: {}", userId);

            // 🔥 추가: 요청 데이터 검증
            if (request.getCartItemId() == null || request.getCartItemId().trim().isEmpty()) {
                log.error("❌ cartItemId 누락");
                return ResponseEntity.badRequest()
                        .body(ApiResponse.<Void>builder()
                                .success(false)
                                .message("cartItemId가 필요합니다.")
                                .build());
            }

            if (request.getQuantity() == null || request.getQuantity() <= 0) {
                log.error("❌ 잘못된 수량: {}", request.getQuantity());
                return ResponseEntity.badRequest()
                        .body(ApiResponse.<Void>builder()
                                .success(false)
                                .message("올바른 수량을 입력해주세요.")
                                .build());
            }

            cartService.updateCartItemQuantity(userId, request.getCartItemId(), request.getQuantity());

            log.info("✅ 장바구니 수량 변경 성공");
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(true)
                    .message("수량이 변경되었습니다.")
                    .build());

        } catch (Exception e) {
            log.error("💥 장바구니 수량 변경 실패: cartItemId={}, error={}",
                    request != null ? request.getCartItemId() : "null", e.getMessage(), e);

            return ResponseEntity.badRequest()
                    .body(ApiResponse.<Void>builder()
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
            @RequestBody Map<String, List<String>> request,
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

            List<String> cartItemIds = request.get("cartItemIds");
            if (cartItemIds == null || cartItemIds.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.<Void>builder()
                                .success(false)
                                .message("삭제할 상품이 선택되지 않았습니다.")
                                .build());
            }

            log.info("장바구니 다중 삭제 - userId: {}, cartItemIds: {}", userId, cartItemIds);

            cartService.removeCartItems(userId, cartItemIds);

            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(true)
                    .message("선택한 상품들이 삭제되었습니다.")
                    .build());

        } catch (Exception e) {
            log.error("장바구니 다중 삭제 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<Void>builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    /**
     * 🔥 구매 완료 후 상품 제거 API (핵심 기능)
     */
    @PostMapping("/remove-purchased-items")
    public ResponseEntity<ApiResponse<Void>> removePurchasedItems(
            @RequestBody Map<String, List<Integer>> request,
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

            List<Integer> productIds = request.get("productIds");
            if (productIds == null || productIds.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.<Void>builder()
                                .success(false)
                                .message("제거할 상품 ID가 제공되지 않았습니다.")
                                .build());
            }

            log.info("🛒 구매 완료 상품 장바구니 제거 - userId: {}, productIds: {}", userId, productIds);

            int removedCount = cartService.removePurchasedItems(userId, productIds);

            log.info("✅ 장바구니에서 {}개 상품 제거 완료", removedCount);

            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(true)
                    .message(String.format("구매 완료된 %d개 상품이 장바구니에서 제거되었습니다.", removedCount))
                    .build());

        } catch (Exception e) {
            log.error("💥 구매 완료 상품 제거 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<Void>builder()
                            .success(false)
                            .message("구매 완료 상품 제거 중 오류가 발생했습니다: " + e.getMessage())
                            .build());
        }
    }

    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Integer>> getCartCount(
            HttpServletRequest httpRequest) {
        try {
            String authHeader = httpRequest.getHeader("Authorization");
            String userId = jwtTokenParser.extractUserIdFromAuthHeader(authHeader);

            if (userId == null) {
                // 게스트 사용자의 경우 0 반환
                return ResponseEntity.ok(ApiResponse.<Integer>builder()
                        .success(true)
                        .message("게스트 장바구니 개수")
                        .data(0)
                        .build());
            }

            log.info("장바구니 개수 조회: userId={}", userId);

            int cartCount = cartService.getCartItemCount(userId);

            return ResponseEntity.ok(ApiResponse.<Integer>builder()
                    .success(true)
                    .message("장바구니 개수 조회 성공")
                    .data(cartCount)
                    .build());

        } catch (Exception e) {
            log.error("장바구니 개수 조회 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<Integer>builder()
                            .success(false)
                            .message("장바구니 개수 조회 중 오류가 발생했습니다: " + e.getMessage())
                            .data(0)
                            .build());
        }
    }
}