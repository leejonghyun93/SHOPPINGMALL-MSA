package org.kosa.commerceservice.controller.cart;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "장바구니 API", description = "장바구니 관리 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Slf4j
public class CartController {

    private final CartService cartService;
    private final JwtTokenParser jwtTokenParser;

    @Operation(summary = "장바구니에 상품 추가", description = "장바구니에 새로운 상품을 추가합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "추가 성공",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<CartItemDTO>> addToCart(
            @Parameter(description = "장바구니 추가 요청 정보", required = true)
            @RequestBody CartRequestDTO request,
            @Parameter(hidden = true) HttpServletRequest httpRequest) {

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

    @Operation(summary = "장바구니 조회", description = "사용자의 장바구니를 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<CartDTO>> getCart(
            @Parameter(hidden = true) HttpServletRequest httpRequest) {
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

    @Operation(summary = "장바구니 수량 변경", description = "장바구니 상품의 수량을 변경합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "변경 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    @PutMapping("/items")
    public ResponseEntity<ApiResponse<Void>> updateCartItem(
            @Parameter(description = "수량 변경 요청 정보", required = true)
            @RequestBody CartUpdateRequestDTO request,
            @Parameter(hidden = true) HttpServletRequest httpRequest) {

        try {
            log.info(" 장바구니 수량 변경 요청 - cartItemId: {}, quantity: {}",
                    request.getCartItemId(), request.getQuantity());

            String authHeader = httpRequest.getHeader("Authorization");
            String userId = jwtTokenParser.extractUserIdFromAuthHeader(authHeader);

            if (userId == null) {
                log.warn(" 인증 실패 - Authorization 헤더: {}", authHeader);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.<Void>builder()
                                .success(false)
                                .message("로그인이 필요합니다.")
                                .build());
            }

            log.info("인증 성공 - userId: {}", userId);

            // 추가: 요청 데이터 검증
            if (request.getCartItemId() == null || request.getCartItemId().trim().isEmpty()) {
                log.error(" cartItemId 누락");
                return ResponseEntity.badRequest()
                        .body(ApiResponse.<Void>builder()
                                .success(false)
                                .message("cartItemId가 필요합니다.")
                                .build());
            }

            if (request.getQuantity() == null || request.getQuantity() <= 0) {
                log.error(" 잘못된 수량: {}", request.getQuantity());
                return ResponseEntity.badRequest()
                        .body(ApiResponse.<Void>builder()
                                .success(false)
                                .message("올바른 수량을 입력해주세요.")
                                .build());
            }

            cartService.updateCartItemQuantity(userId, request.getCartItemId(), request.getQuantity());

            log.info(" 장바구니 수량 변경 성공");
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(true)
                    .message("수량이 변경되었습니다.")
                    .build());

        } catch (Exception e) {
            log.error(" 장바구니 수량 변경 실패: cartItemId={}, error={}",
                    request != null ? request.getCartItemId() : "null", e.getMessage(), e);

            return ResponseEntity.badRequest()
                    .body(ApiResponse.<Void>builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    @Operation(summary = "장바구니 상품 삭제", description = "장바구니에서 특정 상품을 삭제합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<ApiResponse<Void>> removeCartItem(
            @Parameter(description = "장바구니 항목 ID", required = true, example = "CART_ITEM_001")
            @PathVariable String cartItemId,
            @Parameter(hidden = true) HttpServletRequest httpRequest) {

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

    @Operation(summary = "장바구니 상품 다중 삭제", description = "장바구니에서 여러 상품을 한번에 삭제합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    @DeleteMapping("/items")
    public ResponseEntity<ApiResponse<Void>> removeCartItems(
            @Parameter(description = "삭제할 장바구니 항목 ID 목록", required = true)
            @RequestBody Map<String, List<String>> request,
            @Parameter(hidden = true) HttpServletRequest httpRequest) {

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

    @Operation(summary = "구매 완료 상품 제거", description = "구매가 완료된 상품들을 장바구니에서 제거합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "제거 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    @PostMapping("/remove-purchased-items")
    public ResponseEntity<ApiResponse<Void>> removePurchasedItems(
            @Parameter(description = "구매 완료된 상품 ID 목록", required = true)
            @RequestBody Map<String, List<Integer>> request,
            @Parameter(hidden = true) HttpServletRequest httpRequest) {

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

            log.info(" 장바구니에서 {}개 상품 제거 완료", removedCount);

            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(true)
                    .message(String.format("구매 완료된 %d개 상품이 장바구니에서 제거되었습니다.", removedCount))
                    .build());

        } catch (Exception e) {
            log.error("구매 완료 상품 제거 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<Void>builder()
                            .success(false)
                            .message("구매 완료 상품 제거 중 오류가 발생했습니다: " + e.getMessage())
                            .build());
        }
    }

    @Operation(summary = "장바구니 상품 개수 조회", description = "사용자의 장바구니에 담긴 상품 개수를 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Integer>> getCartCount(
            @Parameter(hidden = true) HttpServletRequest httpRequest) {
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