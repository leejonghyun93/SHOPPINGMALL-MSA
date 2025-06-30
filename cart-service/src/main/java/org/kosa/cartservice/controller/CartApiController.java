package org.kosa.cartservice.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kosa.cartservice.dto.*;
import org.kosa.cartservice.service.CartService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Slf4j
public class CartApiController {

    private final CartService cartService;

    @PostMapping
    public ResponseEntity<ApiResponse<CartItemDTO>> addToCart(
            @RequestBody CartRequestDTO request,
            @RequestHeader(value = "X-Username", required = false) String headerUsername,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @RequestHeader(value = "X-User-Name", required = false) String headerUserName,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest httpRequest) {

        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.<CartItemDTO>builder()
                                .success(false)
                                .message("로그인이 필요합니다.")
                                .build());
            }

            if (request.getProductId() == null) {
                throw new IllegalArgumentException("상품 ID가 필요합니다.");
            }

            if (request.getQuantity() == null || request.getQuantity() <= 0) {
                throw new IllegalArgumentException("올바른 수량을 입력해주세요.");
            }

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            String userId;
            try {
                userId = getUserId(authentication, headerUsername, headerUserId, request.getUserId());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.<CartItemDTO>builder()
                                .success(false)
                                .message("로그인이 필요합니다.")
                                .build());
            }

            CartItemDTO result = cartService.addToCart(userId, request);

            return ResponseEntity.ok(ApiResponse.<CartItemDTO>builder()
                    .success(true)
                    .message("장바구니에 상품이 추가되었습니다.")
                    .data(result)
                    .build());

        } catch (Exception e) {
            log.error("장바구니 추가 실패: {}", e.getMessage(), e);

            if (e.getMessage() != null && e.getMessage().contains("로그인")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.<CartItemDTO>builder()
                                .success(false)
                                .message("로그인이 필요합니다.")
                                .build());
            }

            return ResponseEntity.badRequest()
                    .body(ApiResponse.<CartItemDTO>builder()
                            .success(false)
                            .message(e.getMessage() != null ? e.getMessage() : "장바구니 추가 중 오류가 발생했습니다.")
                            .build());
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<CartDTO>> getCart(
            Authentication authentication,
            @RequestHeader(value = "X-Username", required = false) String headerUsername,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @RequestParam(value = "userId", required = false) String paramUserId) {
        try {
            String userId = getUserId(authentication, headerUsername, headerUserId, paramUserId);
            CartDTO cart = cartService.getCart(userId);

            return ResponseEntity.ok(ApiResponse.<CartDTO>builder()
                    .success(true)
                    .message("장바구니 조회 성공")
                    .data(cart)
                    .build());

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.<CartDTO>builder()
                            .success(false)
                            .message("로그인이 필요합니다.")
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
            Authentication authentication,
            @RequestBody CartUpdateRequestDTO request,
            @RequestHeader(value = "X-Username", required = false) String headerUsername,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId) {
        try {
            String userId = getUserId(authentication, headerUsername, headerUserId, request.getUserId());

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
            Authentication authentication,
            @PathVariable String cartItemId,
            @RequestHeader(value = "X-Username", required = false) String headerUsername,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @RequestParam(value = "userId", required = false) String paramUserId) {
        try {
            String userId = getUserId(authentication, headerUsername, headerUserId, paramUserId);

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
            Authentication authentication,
            @RequestBody Map<String, Object> request,
            @RequestHeader(value = "X-Username", required = false) String headerUsername,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId) {
        try {
            @SuppressWarnings("unchecked")
            List<String> cartItemIds = (List<String>) request.get("cartItemIds");
            String requestUserId = (String) request.get("userId");

            String userId = getUserId(authentication, headerUsername, headerUserId, requestUserId);

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
    public ResponseEntity<ApiResponse<Void>> clearCart(
            Authentication authentication,
            @RequestHeader(value = "X-Username", required = false) String headerUsername,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @RequestParam(value = "userId", required = false) String paramUserId) {
        try {
            String userId = getUserId(authentication, headerUsername, headerUserId, paramUserId);

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
    public ResponseEntity<ApiResponse<Integer>> getCartItemCount(
            Authentication authentication,
            @RequestHeader(value = "X-Username", required = false) String headerUsername,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @RequestParam(value = "userId", required = false) String paramUserId) {
        try {
            String userId = getUserId(authentication, headerUsername, headerUserId, paramUserId);
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
            HttpServletRequest httpRequest,
            @RequestHeader(value = "X-Username", required = false) String headerUsername,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId) {
        try {
            List<Long> productIds = request.get("productIds");

            if (productIds == null || productIds.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "제거할 상품 ID가 없습니다"));
            }

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            String userId = null;

            if (headerUsername != null && !headerUsername.trim().isEmpty()) {
                userId = headerUsername;
            } else if (headerUserId != null && !headerUserId.trim().isEmpty()) {
                userId = headerUserId;
            } else if (authentication != null && authentication.isAuthenticated()
                    && !"anonymousUser".equals(authentication.getName())) {
                userId = authentication.getName();
            } else {
                userId = getUserIdFromToken(httpRequest);
            }

            if (userId == null || userId.trim().isEmpty()) {
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

    private String getUserId(Authentication authentication, String headerUsername, String headerUserId, String requestUserId) {
        String userId = null;

        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getName())
                && authentication.getName() != null
                && !authentication.getName().trim().isEmpty()) {
            userId = authentication.getName();
            return userId;
        }

        userId = extractUserIdFromToken();
        if (userId != null && !userId.trim().isEmpty()) {
            return userId;
        }

        if (headerUsername != null && !headerUsername.trim().isEmpty()) {
            return headerUsername;
        }

        if (headerUserId != null && !headerUserId.trim().isEmpty()) {
            return headerUserId;
        }

        if (requestUserId != null && !requestUserId.trim().isEmpty()) {
            return requestUserId;
        }

        throw new IllegalArgumentException("로그인이 필요합니다.");
    }

    private String extractUserIdFromToken() {
        try {
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
            String authHeader = request.getHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return null;
            }

            String token = authHeader.substring(7).trim();
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return null;
            }

            String payload = parts[1];

            payload = payload.replace('-', '+').replace('_', '/');
            while (payload.length() % 4 != 0) {
                payload += "=";
            }

            byte[] decodedBytes = Base64.getDecoder().decode(payload);
            String decodedPayload = new String(decodedBytes, StandardCharsets.UTF_8);

            String userId = extractSubFromJsonString(decodedPayload);

            if (userId != null && !userId.trim().isEmpty()) {
                return userId;
            }

            return null;

        } catch (Exception e) {
            return null;
        }
    }

    private String extractSubFromJsonString(String json) {
        try {
            String[] patterns = {"\"sub\":", "\"username\":", "\"userId\":", "\"user_id\":"};

            for (String pattern : patterns) {
                int startIndex = json.indexOf(pattern);
                if (startIndex != -1) {
                    startIndex += pattern.length();

                    while (startIndex < json.length() &&
                            (json.charAt(startIndex) == ' ' || json.charAt(startIndex) == '"')) {
                        startIndex++;
                    }

                    int endIndex = startIndex;
                    while (endIndex < json.length() &&
                            json.charAt(endIndex) != '"' &&
                            json.charAt(endIndex) != ',' &&
                            json.charAt(endIndex) != '}') {
                        endIndex++;
                    }

                    if (endIndex > startIndex) {
                        String value = json.substring(startIndex, endIndex).trim();
                        if (!value.isEmpty() && !"null".equals(value)) {
                            return value;
                        }
                    }
                }
            }

            return null;

        } catch (Exception e) {
            return null;
        }
    }

    private String getUserIdFromToken(HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return null;
            }

            String token = authHeader.substring(7);
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return null;
            }

            String payload = parts[1];
            byte[] decodedBytes = Base64.getDecoder().decode(payload);
            String decodedPayload = new String(decodedBytes);

            if (decodedPayload.contains("\"sub\":")) {
                int startIndex = decodedPayload.indexOf("\"sub\":\"") + 7;
                int endIndex = decodedPayload.indexOf("\"", startIndex);
                if (startIndex > 6 && endIndex > startIndex) {
                    String userId = decodedPayload.substring(startIndex, endIndex);
                    return userId;
                }
            }

            return null;

        } catch (Exception e) {
            return null;
        }
    }
}