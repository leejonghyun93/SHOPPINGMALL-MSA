package org.kosa.cartservice.controller;

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

import java.util.Base64;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Slf4j
public class CartApiController {

    private final CartService cartService;

    /**
     * 장바구니에 상품 추가 - 헤더 디버깅 포함
     */
    @PostMapping
    public ResponseEntity<ApiResponse<CartItemDTO>> addToCart(
            @RequestBody CartRequestDTO request,
            @RequestHeader(value = "X-Username", required = false) String headerUsername,  // ✅ 수정
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @RequestHeader(value = "X-User-Name", required = false) String headerUserName,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest httpRequest) {

        // 🔍 모든 헤더 출력 (디버깅용)
        log.info("=== 장바구니 추가 요청 디버깅 ===");
        log.info("Authorization: {}", authHeader != null ? "Bearer ***" : "없음");
        log.info("X-Username: {}", headerUsername);
        log.info("X-User-Id: {}", headerUserId);
        log.info("X-User-Name: {}", headerUserName);

        // 모든 헤더 출력
        Enumeration<String> headerNames = httpRequest.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = httpRequest.getHeader(headerName);
            log.info("헤더: {} = {}", headerName, headerValue);
        }

        log.info("요청 데이터: {}", request);

        try {
            // 입력 데이터 검증
            if (request.getProductId() == null || request.getProductId().trim().isEmpty()) {
                throw new IllegalArgumentException("상품 ID가 필요합니다.");
            }

            if (request.getQuantity() == null || request.getQuantity() <= 0) {
                throw new IllegalArgumentException("올바른 수량을 입력해주세요.");
            }

            // 인증 정보 가져오기 (동기 방식)
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            // ✅ 수정: Gateway에서 보낸 X-Username 헤더 우선 사용
            String userId = getUserId(authentication, headerUsername, headerUserId, request.getUserId());

            log.info("최종 결정된 사용자 ID: {}", userId);
            log.info("장바구니 추가 처리: userId={}, productId={}, quantity={}",
                    userId, request.getProductId(), request.getQuantity());

            // 서비스 호출
            CartItemDTO result = cartService.addToCart(userId, request);

            log.info("장바구니 추가 성공: result={}", result);

            ApiResponse<CartItemDTO> response = ApiResponse.<CartItemDTO>builder()
                    .success(true)
                    .message("장바구니에 상품이 추가되었습니다.")
                    .data(result)
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("장바구니 추가 실패: {}", e.getMessage(), e);

            String errorMessage = e.getMessage();
            if (errorMessage == null || errorMessage.trim().isEmpty()) {
                errorMessage = "장바구니 추가 중 오류가 발생했습니다.";
            }

            ApiResponse<CartItemDTO> errorResponse = ApiResponse.<CartItemDTO>builder()
                    .success(false)
                    .message(errorMessage)
                    .build();

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 장바구니 조회 - 헤더 수정
     */
    @GetMapping
    public ResponseEntity<ApiResponse<CartDTO>> getCart(
            Authentication authentication,
            @RequestHeader(value = "X-Username", required = false) String headerUsername,  // ✅ 수정
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @RequestParam(value = "userId", required = false) String paramUserId) {
        try {
            String userId = getUserId(authentication, headerUsername, headerUserId, paramUserId);
            log.info("장바구니 조회 요청: userId={}", userId);

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

    /**
     * 장바구니 상품 수량 변경 - 헤더 수정
     */
    @PutMapping("/items")
    public ResponseEntity<ApiResponse<CartItemDTO>> updateCartItemQuantity(
            Authentication authentication,
            @RequestBody CartUpdateRequestDTO request,
            @RequestHeader(value = "X-Username", required = false) String headerUsername,  // ✅ 수정
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId) {
        try {
            String userId = getUserId(authentication, headerUsername, headerUserId, request.getUserId());
            log.info("장바구니 수량 변경 요청: userId={}, cartItemId={}, quantity={}",
                    userId, request.getCartItemId(), request.getQuantity());

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

    /**
     * 장바구니 상품 삭제 - 헤더 수정
     */
    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<ApiResponse<Void>> removeCartItem(
            Authentication authentication,
            @PathVariable String cartItemId,
            @RequestHeader(value = "X-Username", required = false) String headerUsername,  // ✅ 수정
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @RequestParam(value = "userId", required = false) String paramUserId) {
        try {
            String userId = getUserId(authentication, headerUsername, headerUserId, paramUserId);
            log.info("장바구니 상품 삭제 요청: userId={}, cartItemId={}", userId, cartItemId);

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

    /**
     * 선택된 장바구니 상품들 삭제 - 헤더 수정
     */
    @DeleteMapping("/items")
    public ResponseEntity<ApiResponse<Void>> removeCartItems(
            Authentication authentication,
            @RequestBody Map<String, Object> request,
            @RequestHeader(value = "X-Username", required = false) String headerUsername,  // ✅ 수정
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId) {
        try {
            @SuppressWarnings("unchecked")
            List<String> cartItemIds = (List<String>) request.get("cartItemIds");
            String requestUserId = (String) request.get("userId");

            String userId = getUserId(authentication, headerUsername, headerUserId, requestUserId);
            log.info("장바구니 상품 일괄 삭제 요청: userId={}, count={}", userId, cartItemIds.size());

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

    /**
     * 장바구니 전체 비우기 - 헤더 수정
     */
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> clearCart(
            Authentication authentication,
            @RequestHeader(value = "X-Username", required = false) String headerUsername,  // ✅ 수정
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @RequestParam(value = "userId", required = false) String paramUserId) {
        try {
            String userId = getUserId(authentication, headerUsername, headerUserId, paramUserId);
            log.info("장바구니 전체 비우기 요청: userId={}", userId);

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

    /**
     * 장바구니 상품 개수 조회 - 헤더 수정
     */
    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Integer>> getCartItemCount(
            Authentication authentication,
            @RequestHeader(value = "X-Username", required = false) String headerUsername,  // ✅ 수정
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

    /**
     * 헬스체크
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Cart Service is running!");
    }

    /**
     * ✅ 수정: 사용자 ID 결정 우선순위
     * 1. 인증된 사용자 (JWT 토큰)
     * 2. 헤더의 X-Username (Gateway에서 JWT 파싱한 username)
     * 3. 헤더의 X-User-Id
     * 4. 요청 파라미터/바디의 userId
     * 5. 게스트 사용자 ID 생성
     */
    private String getUserId(Authentication authentication, String headerUsername, String headerUserId, String requestUserId) {
        // 1. 인증된 사용자
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getName())) {
            log.info("✅ 인증된 사용자: {}", authentication.getName());
            return authentication.getName();
        }

        // 2. 헤더의 X-Username (Gateway에서 JWT 파싱한 결과)
        if (headerUsername != null && !headerUsername.trim().isEmpty()) {
            log.info("✅ Gateway JWT 파싱 결과 사용자 ID: {}", headerUsername);
            return headerUsername;
        }

        // 3. 헤더의 X-User-Id
        if (headerUserId != null && !headerUserId.trim().isEmpty()) {
            log.info("✅ 헤더에서 사용자 ID: {}", headerUserId);
            return headerUserId;
        }

        // 4. 요청의 사용자 ID
        if (requestUserId != null && !requestUserId.trim().isEmpty()) {
            log.info("✅ 요청에서 사용자 ID: {}", requestUserId);
            return requestUserId;
        }

        // 5. 게스트 사용자 ID 생성
        String guestId = "guest_" + System.currentTimeMillis();
        log.info("✅ 게스트 사용자 ID 생성: {}", guestId);
        return guestId;
    }

    /**
     * HTTP 요청에서 사용자 ID 추출 (JWT 토큰에서)
     * @param request HTTP 요청
     * @return 사용자 ID
     */
    private String getUserIdFromToken(HttpServletRequest request) {
        try {
            // Authorization 헤더에서 토큰 추출
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("Authorization 헤더가 없거나 형식이 잘못됨");
                return null;
            }

            String token = authHeader.substring(7); // "Bearer " 제거

            // JWT 토큰 파싱하여 사용자 ID 추출
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                log.warn("JWT 토큰 형식이 잘못됨");
                return null;
            }

            // Base64 디코딩
            String payload = parts[1];
            byte[] decodedBytes = Base64.getDecoder().decode(payload);
            String decodedPayload = new String(decodedBytes);

            // JSON 파싱하여 사용자 ID 추출 (간단한 방법)
            if (decodedPayload.contains("\"sub\":")) {
                int startIndex = decodedPayload.indexOf("\"sub\":\"") + 7;
                int endIndex = decodedPayload.indexOf("\"", startIndex);
                if (startIndex > 6 && endIndex > startIndex) {
                    String userId = decodedPayload.substring(startIndex, endIndex);
                    log.info("토큰에서 추출된 사용자 ID: {}", userId);
                    return userId;
                }
            }

            log.warn("토큰에서 사용자 ID를 찾을 수 없음");
            return null;

        } catch (Exception e) {
            log.error("토큰에서 사용자 ID 추출 실패: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 🔥 수정된 구매 완료 상품 제거 엔드포인트
     */
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

            // 인증 정보 가져오기
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            // 여러 방법으로 사용자 ID 확인
            String userId = null;

            // 1. 헤더에서 사용자 ID 확인
            if (headerUsername != null && !headerUsername.trim().isEmpty()) {
                userId = headerUsername;
            } else if (headerUserId != null && !headerUserId.trim().isEmpty()) {
                userId = headerUserId;
            }
            // 2. 인증 정보에서 확인
            else if (authentication != null && authentication.isAuthenticated()
                    && !"anonymousUser".equals(authentication.getName())) {
                userId = authentication.getName();
            }
            // 3. 토큰에서 직접 추출
            else {
                userId = getUserIdFromToken(httpRequest);
            }

            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "사용자 인증이 필요합니다"));
            }

            log.info("구매 완료 상품 장바구니 제거 요청: userId={}, productIds={}", userId, productIds);

            // 서비스 호출
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