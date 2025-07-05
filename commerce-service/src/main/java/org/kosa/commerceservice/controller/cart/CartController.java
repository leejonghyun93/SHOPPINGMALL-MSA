package org.kosa.commerceservice.controller.cart;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.kosa.commerceservice.dto.ApiResponse;
import org.kosa.commerceservice.dto.cart.CartDTO;
import org.kosa.commerceservice.dto.cart.CartItemDTO;
import org.kosa.commerceservice.dto.cart.CartRequestDTO;
import org.kosa.commerceservice.service.cart.CartService;
import org.kosa.commerceservice.util.JwtTokenParser;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Slf4j
public class CartController {

    private final CartService cartService;
    private final JwtTokenParser jwtTokenParser;

//    @GetMapping("/count")
//    public ResponseEntity<ApiResponse<Integer>> getCartCount(
//            HttpServletRequest httpRequest) {
//        try {
//            String authHeader = httpRequest.getHeader("Authorization");
//            String userId = jwtTokenParser.extractUserIdFromAuthHeader(authHeader);
//
//            if (userId == null) {
//                // 게스트 사용자의 경우 0 반환
//                return ResponseEntity.ok(ApiResponse.<Integer>builder()
//                        .success(true)
//                        .message("게스트 장바구니 개수")
//                        .data(0)
//                        .build());
//            }
//
//            log.info("장바구니 개수 조회: userId={}", userId);
//
//            int cartCount = cartService.getCartItemCount(userId);
//
//            return ResponseEntity.ok(ApiResponse.<Integer>builder()
//                    .success(true)
//                    .message("장바구니 개수 조회 성공")
//                    .data(cartCount)
//                    .build());
//
//        } catch (Exception e) {
//            log.error("장바구니 개수 조회 실패: {}", e.getMessage(), e);
//            return ResponseEntity.badRequest()
//                    .body(ApiResponse.<Integer>builder()
//                            .success(false)
//                            .message("장바구니 개수 조회 중 오류가 발생했습니다: " + e.getMessage())
//                            .data(0)
//                            .build());
//        }
//    }

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