package org.kosa.cartservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kosa.cartservice.client.ProductServiceClient;
import org.kosa.cartservice.dto.*;
import org.kosa.cartservice.entity.Cart;
import org.kosa.cartservice.entity.CartItem;
import org.kosa.cartservice.mapper.CartItemRepository;
import org.kosa.cartservice.mapper.CartRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductServiceClient productServiceClient;

    @Transactional
    public CartItemDTO addToCart(String userId, CartRequestDTO request) {
        try {
            if (userId == null || userId.trim().isEmpty()) {
                throw new IllegalArgumentException("사용자 ID가 필요합니다.");
            }

            Optional<Cart> existingCartOpt = cartRepository.findByUserIdAndStatus(userId, "ACTIVE");

            Cart cart;
            if (existingCartOpt.isPresent()) {
                cart = existingCartOpt.get();
            } else {
                cart = Cart.builder()
                        .userId(userId)
                        .status("ACTIVE")
                        .cartItems(new ArrayList<>())
                        .build();

                try {
                    cart = cartRepository.save(cart);
                    Optional<Cart> verifyCart = cartRepository.findById(cart.getCartId());
                    if (!verifyCart.isPresent()) {
                        throw new RuntimeException("장바구니 생성 후 검증 실패");
                    }
                } catch (Exception e) {
                    throw new RuntimeException("장바구니 생성에 실패했습니다: " + e.getMessage(), e);
                }
            }

            if (cart == null || cart.getCartId() == null) {
                throw new RuntimeException("유효하지 않은 장바구니 상태");
            }

            Optional<CartItem> existingItemOpt = cartItemRepository
                    .findByCartIdAndProductIdAndProductOptionId(
                            cart.getCartId(),
                            request.getProductId(),
                            request.getProductOptionId()
                    );

            CartItem cartItem;

            if (existingItemOpt.isPresent()) {
                cartItem = existingItemOpt.get();
                Integer oldQuantity = cartItem.getQuantity();
                cartItem.setQuantity(oldQuantity + request.getQuantity());
            } else {
                cartItem = CartItem.builder()
                        .cartId(cart.getCartId())
                        .productId(request.getProductId())
                        .productOptionId(request.getProductOptionId())
                        .quantity(request.getQuantity())
                        .cart(cart)
                        .build();

                if (cart.getCartItems() == null) {
                    cart.setCartItems(new ArrayList<>());
                }
                cart.getCartItems().add(cartItem);
            }

            try {
                CartItem savedItem = cartItemRepository.save(cartItem);

                try {
                    cartRepository.save(cart);
                } catch (Exception e) {
                    // CartItem은 이미 저장되었으므로 계속 진행
                }

                CartItemDTO result = CartItemDTO.builder()
                        .cartItemId(savedItem.getCartItemId())
                        .cartId(savedItem.getCartId())
                        .productId(savedItem.getProductId())
                        .productOptionId(savedItem.getProductOptionId())
                        .quantity(savedItem.getQuantity())
                        .updatedDate(savedItem.getUpdatedDate())
                        .build();

                return result;

            } catch (Exception e) {
                throw new RuntimeException("장바구니 아이템 저장에 실패했습니다: " + e.getMessage(), e);
            }

        } catch (Exception e) {
            throw new RuntimeException("장바구니 추가에 실패했습니다: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public CartDTO getCart(String userId) {
        Cart cart = cartRepository.findActiveCartByUserId(userId).orElse(null);

        if (cart == null) {
            return CartDTO.builder()
                    .userId(userId)
                    .cartItems(List.of())
                    .totalItems(0)
                    .totalPrice(0L)
                    .totalDiscountPrice(0L)
                    .finalPrice(0L)
                    .deliveryFee(0L)
                    .build();
        }

        List<CartItem> cartItems = cartItemRepository.findByCartId(cart.getCartId());

        List<CartItemDTO> cartItemDTOs = cartItems.stream()
                .map(item -> {
                    try {
                        ProductDTO product = productServiceClient.getProduct(item.getProductId());
                        return convertToCartItemDTO(item, product);
                    } catch (Exception e) {
                        return convertToCartItemDTOWithoutProduct(item);
                    }
                })
                .collect(Collectors.toList());

        CartDTO result = calculateCartTotals(cart, cartItemDTOs);
        return result;
    }

    public CartItemDTO updateCartItemQuantity(String userId, CartUpdateRequestDTO request) {
        CartItem cartItem = cartItemRepository.findById(request.getCartItemId())
                .orElseThrow(() -> new RuntimeException("장바구니 상품을 찾을 수 없습니다: " + request.getCartItemId()));

        Cart cart = cartRepository.findById(cartItem.getCartId())
                .orElseThrow(() -> new RuntimeException("장바구니를 찾을 수 없습니다"));

        if (!cart.getUserId().equals(userId)) {
            throw new RuntimeException("접근 권한이 없습니다");
        }

        if (request.getQuantity() <= 0) {
            cartItemRepository.delete(cartItem);
            return null;
        }

        cartItem.setQuantity(request.getQuantity());
        cartItemRepository.save(cartItem);

        try {
            ProductDTO product = productServiceClient.getProduct(cartItem.getProductId());
            return convertToCartItemDTO(cartItem, product);
        } catch (Exception e) {
            return convertToCartItemDTOWithoutProduct(cartItem);
        }
    }

    public void removeCartItem(String userId, String cartItemId) {
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("장바구니 상품을 찾을 수 없습니다: " + cartItemId));

        Cart cart = cartRepository.findById(cartItem.getCartId())
                .orElseThrow(() -> new RuntimeException("장바구니를 찾을 수 없습니다"));

        if (!cart.getUserId().equals(userId)) {
            throw new RuntimeException("접근 권한이 없습니다");
        }

        cartItemRepository.delete(cartItem);
    }

    public void removeCartItems(String userId, List<String> cartItemIds) {
        for (String cartItemId : cartItemIds) {
            removeCartItem(userId, cartItemId);
        }
    }

    public void clearCart(String userId) {
        Cart cart = cartRepository.findActiveCartByUserId(userId)
                .orElseThrow(() -> new RuntimeException("장바구니를 찾을 수 없습니다"));

        List<CartItem> cartItems = cartItemRepository.findByCartIdOrderByUpdatedDateDesc(cart.getCartId());
        cartItemRepository.deleteAll(cartItems);
    }

    private CartItemDTO convertToCartItemDTO(CartItem cartItem, ProductDTO product) {
        Long salePrice = product.getSalePrice() != null ? product.getSalePrice() : product.getPrice();
        Integer discountRate = product.getDiscountRate() != null ? product.getDiscountRate() : 0;

        return CartItemDTO.builder()
                .cartItemId(cartItem.getCartItemId())
                .cartId(cartItem.getCartId())
                .productId(cartItem.getProductId())
                .productOptionId(cartItem.getProductOptionId())
                .quantity(cartItem.getQuantity())
                .updatedDate(cartItem.getUpdatedDate())
                .productName(product.getName() != null ? product.getName() : product.getTitle())
                .productImage(getProductMainImage(product))
                .productPrice(product.getPrice())
                .salePrice(salePrice)
                .discountRate(discountRate)
                .category(getProductCategory(product))
                .deliveryType("dawn")
                .itemTotalPrice(product.getPrice() * cartItem.getQuantity())
                .itemTotalSalePrice(salePrice * cartItem.getQuantity())
                .build();
    }

    private CartItemDTO convertToCartItemDTOWithoutProduct(CartItem cartItem) {
        return CartItemDTO.builder()
                .cartItemId(cartItem.getCartItemId())
                .cartId(cartItem.getCartId())
                .productId(cartItem.getProductId())
                .productOptionId(cartItem.getProductOptionId())
                .quantity(cartItem.getQuantity())
                .updatedDate(cartItem.getUpdatedDate())
                .productName("상품 정보 없음")
                .productImage("/api/placeholder/120/120")
                .productPrice(0L)
                .salePrice(0L)
                .discountRate(0)
                .category("normal")
                .deliveryType("dawn")
                .itemTotalPrice(0L)
                .itemTotalSalePrice(0L)
                .build();
    }

    private CartDTO calculateCartTotals(Cart cart, List<CartItemDTO> cartItems) {
        Integer totalItems = cartItems.stream()
                .mapToInt(CartItemDTO::getQuantity)
                .sum();

        Long totalPrice = cartItems.stream()
                .mapToLong(CartItemDTO::getItemTotalPrice)
                .sum();

        Long totalSalePrice = cartItems.stream()
                .mapToLong(CartItemDTO::getItemTotalSalePrice)
                .sum();

        Long totalDiscountPrice = totalPrice - totalSalePrice;

        Long deliveryFee = totalSalePrice >= 40000L ? 0L : 3000L;

        Long finalPrice = totalSalePrice + deliveryFee;

        return CartDTO.builder()
                .cartId(cart.getCartId())
                .userId(cart.getUserId())
                .status(cart.getStatus())
                .createdDate(cart.getCreatedDate())
                .updatedDate(cart.getUpdatedDate())
                .cartItems(cartItems)
                .totalItems(totalItems)
                .totalPrice(totalPrice)
                .totalDiscountPrice(totalDiscountPrice)
                .finalPrice(finalPrice)
                .deliveryFee(deliveryFee)
                .build();
    }

    private String getProductMainImage(ProductDTO product) {
        if (product.getImages() != null && !product.getImages().isEmpty()) {
            return product.getImages().get(0);
        }
        return product.getMainImage();
    }

    private String getProductCategory(ProductDTO product) {
        if (product.getCategory() != null &&
                (product.getCategory().contains("냉동") || product.getCategory().contains("아이스크림"))) {
            return "frozen";
        }
        return "normal";
    }

    @Transactional
    public void removePurchasedItems(String userId, List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return;
        }

        try {
            Optional<Cart> cartOpt = cartRepository.findByUserIdAndStatus(userId, "ACTIVE");
            if (!cartOpt.isPresent()) {
                cartOpt = cartRepository.findByUserId(userId);
                if (!cartOpt.isPresent()) {
                    return;
                }
            }

            Cart cart = cartOpt.get();

            List<String> stringProductIds = productIds.stream()
                    .map(String::valueOf)
                    .collect(Collectors.toList());

            List<CartItem> allCartItems = cartItemRepository.findByCartId(cart.getCartId());

            List<CartItem> itemsToRemove = allCartItems.stream()
                    .filter(item -> stringProductIds.contains(item.getProductId()))
                    .collect(Collectors.toList());

            if (itemsToRemove.isEmpty()) {
                return;
            }

            List<String> deletedItemIds = new ArrayList<>();

            for (CartItem item : itemsToRemove) {
                try {
                    cart.getCartItems().remove(item);
                    cartItemRepository.delete(item);
                    deletedItemIds.add(item.getCartItemId());
                } catch (Exception e) {
                    // 개별 삭제 실패는 무시하고 계속 진행
                }
            }

            cart.setUpdatedDate(LocalDateTime.now());
            cartRepository.save(cart);

        } catch (Exception e) {
            throw new RuntimeException("장바구니 정리 중 오류가 발생했습니다.", e);
        }
    }
    /**
     * 🔥 수정된 JWT 파싱 로직 - 숫자/문자열 모두 지원
     */
    private String extractUserIdFromRequest() {
        try {
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
            String authHeader = request.getHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("Authorization 헤더가 없거나 Bearer 형식이 아님");
                return null;
            }

            String token = authHeader.substring(7).trim();
            return parseJwtPayload(token);

        } catch (Exception e) {
            log.error("Request에서 JWT 추출 실패: {}", e.getMessage());
            return null;
        }
    }

    /**
     * JWT 토큰에서 사용자 ID 추출 (숫자/문자열 모두 지원)
     */
    private String parseJwtPayload(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                log.warn("JWT 토큰 형식이 잘못됨 - parts: {}", parts.length);
                return null;
            }

            String payload = parts[1];

            // Base64URL → Base64 변환
            String base64Payload = base64UrlToBase64(payload);

            // Base64 디코딩
            byte[] decodedBytes = Base64.getDecoder().decode(base64Payload);
            String decodedPayload = new String(decodedBytes, StandardCharsets.UTF_8);

            log.debug("디코딩된 JWT payload: {}", decodedPayload);

            // JSON 파싱
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(decodedPayload);

            // 🔥 중요: 숫자/문자열 상관없이 사용자 ID 추출
            String userId = extractUserIdFromJson(jsonNode);

            if (userId != null && !userId.trim().isEmpty()) {
                log.info("JWT에서 추출된 사용자 ID: {}", userId);
                return userId;
            }

            log.warn("JWT payload에서 사용자 ID를 찾을 수 없음");
            return null;

        } catch (Exception e) {
            log.error("JWT payload 파싱 실패: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Base64URL을 Base64로 변환
     */
    private String base64UrlToBase64(String base64Url) {
        String base64 = base64Url
                .replace('-', '+')
                .replace('_', '/');

        while (base64.length() % 4 != 0) {
            base64 += "=";
        }

        return base64;
    }

    /**
     * 🔥 수정: JSON에서 사용자 ID 추출 (숫자/문자열 모두 허용)
     */
    private String extractUserIdFromJson(JsonNode jsonNode) {
        // 우선순위: sub > username > userId > user_id > email > id
        String[] userIdFields = {"sub", "username", "userId", "user_id", "email", "id"};

        for (String field : userIdFields) {
            if (jsonNode.has(field)) {
                String value = jsonNode.get(field).asText();
                if (value != null && !value.trim().isEmpty() && !"null".equals(value)) {
                    log.debug("JWT에서 {} 필드로 사용자 ID 추출: {}", field, value);
                    return value; // 🔥 숫자든 문자열이든 그대로 반환
                }
            }
        }

        return null;
    }

    /**
     * 🔥 기존 getUserIdFromToken 메서드 완전 수정
     */
    private String getUserIdFromToken(HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("Authorization 헤더가 없거나 형식이 잘못됨");
                return null;
            }

            String token = authHeader.substring(7).trim();
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                log.warn("JWT 토큰 형식이 잘못됨");
                return null;
            }

            String payload = parts[1];

            // Base64URL → Base64 변환
            payload = payload.replace('-', '+').replace('_', '/');
            while (payload.length() % 4 != 0) {
                payload += "=";
            }

            byte[] decodedBytes = Base64.getDecoder().decode(payload);
            String decodedPayload = new String(decodedBytes, StandardCharsets.UTF_8);

            log.debug("디코딩된 JWT payload: {}", decodedPayload);

            // 🔥 수정: 간단한 JSON 파싱으로 sub 값 추출 (숫자/문자열 무관)
            String userId = extractSubFromJsonString(decodedPayload);

            if (userId != null && !userId.trim().isEmpty()) {
                log.info("토큰에서 추출된 사용자 ID: {}", userId);
                return userId;
            }

            log.warn("토큰에서 사용자 ID를 찾을 수 없음");
            return null;

        } catch (Exception e) {
            log.error("토큰에서 사용자 ID 추출 실패: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 🔥 수정: JSON 문자열에서 사용자 ID 추출 (숫자/문자열 모두 허용)
     */
    private String extractSubFromJsonString(String json) {
        try {
            // 여러 필드 패턴 시도
            String[] patterns = {"\"sub\":", "\"username\":", "\"userId\":", "\"user_id\":"};

            for (String pattern : patterns) {
                int startIndex = json.indexOf(pattern);
                if (startIndex != -1) {
                    startIndex += pattern.length();

                    // 공백과 따옴표 건너뛰기
                    while (startIndex < json.length() &&
                            (json.charAt(startIndex) == ' ' || json.charAt(startIndex) == '"')) {
                        startIndex++;
                    }

                    // 값의 끝 찾기
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
                            // 🔥 중요: 숫자든 문자열이든 그대로 반환
                            log.debug("JSON에서 {} 패턴으로 사용자 ID 추출: {}", pattern, value);
                            return value;
                        }
                    }
                }
            }

            return null;

        } catch (Exception e) {
            log.error("JSON에서 사용자 ID 추출 실패: {}", e.getMessage());
            return null;
        }
    }
}