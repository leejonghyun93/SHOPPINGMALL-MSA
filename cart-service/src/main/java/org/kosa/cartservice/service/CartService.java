package org.kosa.cartservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kosa.cartservice.client.ProductServiceClient;
import org.kosa.cartservice.dto.*;
import org.kosa.cartservice.mapper.CartItemRepository;
import org.kosa.cartservice.mapper.CartRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductServiceClient productServiceClient;

    /**
     * 장바구니에 상품 추가 - 디버깅 강화
     */
    @Transactional
    public CartItemDTO addToCart(String userId, CartRequestDTO request) {
        try {
            log.info("=== 장바구니 추가 서비스 시작 ===");
            log.info("userId: {}", userId);
            log.info("productId: {}", request.getProductId());
            log.info("quantity: {}", request.getQuantity());
            log.info("productOptionId: {}", request.getProductOptionId());

            // ✅ 1. 사용자 ID 유효성 검증
            if (userId == null || userId.trim().isEmpty()) {
                throw new IllegalArgumentException("사용자 ID가 필요합니다.");
            }

            // ✅ 2. 기존 장바구니 조회
            log.info("기존 장바구니 조회 시도: userId={}", userId);
            Optional<Cart> existingCartOpt = cartRepository.findByUserIdAndStatus(userId, "ACTIVE");

            Cart cart;
            if (existingCartOpt.isPresent()) {
                cart = existingCartOpt.get();
                log.info("기존 장바구니 발견: cartId={}, userId={}", cart.getCartId(), cart.getUserId());
            } else {
                // ✅ 3. 새 장바구니 생성
                log.info("새 장바구니 생성 시작: userId={}", userId);

                cart = Cart.builder()
                        .userId(userId)
                        .status("ACTIVE")
                        .cartItems(new ArrayList<>())
                        .build();

                try {
                    cart = cartRepository.save(cart);
                    log.info("새 장바구니 생성 성공: cartId={}, userId={}", cart.getCartId(), cart.getUserId());

                    // ✅ 생성 직후 다시 조회해서 확인
                    Optional<Cart> verifyCart = cartRepository.findById(cart.getCartId());
                    if (verifyCart.isPresent()) {
                        log.info("생성된 장바구니 검증 성공: {}", verifyCart.get().getCartId());
                    } else {
                        log.error("생성된 장바구니 검증 실패!");
                        throw new RuntimeException("장바구니 생성 후 검증 실패");
                    }

                } catch (Exception e) {
                    log.error("장바구니 생성 실패: {}", e.getMessage(), e);
                    throw new RuntimeException("장바구니 생성에 실패했습니다: " + e.getMessage(), e);
                }
            }

            // ✅ 4. Cart 객체 상태 확인
            if (cart == null || cart.getCartId() == null) {
                log.error("Cart 객체가 null이거나 cartId가 null입니다. cart: {}", cart);
                throw new RuntimeException("유효하지 않은 장바구니 상태");
            }

            log.info("장바구니 확보 완료: cartId={}, userId={}", cart.getCartId(), cart.getUserId());

            // ✅ 5. 기존 CartItem 확인
            log.info("기존 CartItem 조회: cartId={}, productId={}, productOptionId={}",
                    cart.getCartId(), request.getProductId(), request.getProductOptionId());

            Optional<CartItem> existingItemOpt = cartItemRepository
                    .findByCartIdAndProductIdAndProductOptionId(
                            cart.getCartId(),
                            request.getProductId(),
                            request.getProductOptionId()
                    );

            CartItem cartItem;

            if (existingItemOpt.isPresent()) {
                // ✅ 6-1. 기존 상품 수량 업데이트
                cartItem = existingItemOpt.get();
                Integer oldQuantity = cartItem.getQuantity();
                cartItem.setQuantity(oldQuantity + request.getQuantity());

                log.info("기존 CartItem 수량 업데이트: cartItemId={}, 기존수량={}, 추가수량={}, 최종수량={}",
                        cartItem.getCartItemId(), oldQuantity, request.getQuantity(), cartItem.getQuantity());
            } else {
                // ✅ 6-2. 새 CartItem 생성
                log.info("새 CartItem 생성 시작");

                cartItem = CartItem.builder()
                        .cartId(cart.getCartId())  // ✅ 확실히 설정
                        .productId(request.getProductId())
                        .productOptionId(request.getProductOptionId())
                        .quantity(request.getQuantity())
                        .cart(cart)  // ✅ 연관관계 설정
                        .build();

                // ✅ 양방향 연관관계 설정
                if (cart.getCartItems() == null) {
                    cart.setCartItems(new ArrayList<>());
                }
                cart.getCartItems().add(cartItem);

                log.info("새 CartItem 생성 완료: productId={}, quantity={}, cartId={}",
                        request.getProductId(), request.getQuantity(), cart.getCartId());
            }

            // ✅ 7. CartItem 저장
            try {
                CartItem savedItem = cartItemRepository.save(cartItem);
                log.info("CartItem 저장 성공: cartItemId={}, cartId={}, productId={}",
                        savedItem.getCartItemId(), savedItem.getCartId(), savedItem.getProductId());

                // ✅ 8. Cart도 다시 저장 (updatedDate 갱신)
                try {
                    Cart savedCart = cartRepository.save(cart);
                    log.info("Cart 업데이트 성공: cartId={}, userId={}, updatedDate={}",
                            savedCart.getCartId(), savedCart.getUserId(), savedCart.getUpdatedDate());
                } catch (Exception e) {
                    log.error("Cart 업데이트 실패: {}", e.getMessage(), e);
                    // CartItem은 이미 저장되었으므로 경고만 로그
                }

                // ✅ 9. DTO 변환하여 반환
                CartItemDTO result = CartItemDTO.builder()
                        .cartItemId(savedItem.getCartItemId())
                        .cartId(savedItem.getCartId())
                        .productId(savedItem.getProductId())
                        .productOptionId(savedItem.getProductOptionId())
                        .quantity(savedItem.getQuantity())
                        .updatedDate(savedItem.getUpdatedDate())
                        .build();

                log.info("=== 장바구니 추가 완료 ===");
                return result;

            } catch (Exception e) {
                log.error("CartItem 저장 실패: {}", e.getMessage(), e);
                throw new RuntimeException("장바구니 아이템 저장에 실패했습니다: " + e.getMessage(), e);
            }

        } catch (Exception e) {
            log.error("장바구니 추가 중 전체 오류 발생: userId={}, productId={}, error={}",
                    userId, request.getProductId(), e.getMessage(), e);
            throw new RuntimeException("장바구니 추가에 실패했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 장바구니 조회 - 기존 유지
     */
    @Transactional(readOnly = true)
    public CartDTO getCart(String userId) {
        log.info("장바구니 조회 시작: userId={}", userId);

        Cart cart = cartRepository.findActiveCartByUserId(userId).orElse(null);

        if (cart == null) {
            log.info("장바구니 없음 - 빈 장바구니 반환: userId={}", userId);
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

        log.info("장바구니 발견: cartId={}, userId={}", cart.getCartId(), cart.getUserId());

        List<CartItem> cartItems = cartItemRepository.findByCartId(cart.getCartId());
        log.info("조회된 CartItem 수: {}", cartItems.size());

        List<CartItemDTO> cartItemDTOs = cartItems.stream()
                .map(item -> {
                    try {
                        ProductDTO product = productServiceClient.getProduct(item.getProductId());
                        return convertToCartItemDTO(item, product);
                    } catch (Exception e) {
                        log.error("상품 정보 조회 실패: {}", item.getProductId());
                        return convertToCartItemDTOWithoutProduct(item);
                    }
                })
                .collect(Collectors.toList());

        CartDTO result = calculateCartTotals(cart, cartItemDTOs);
        log.info("장바구니 조회 완료: totalItems={}", result.getTotalItems());
        return result;
    }

    // ✅ 나머지 메서드들은 기존과 동일하게 유지...

    /**
     * 장바구니 상품 수량 변경
     */
    public CartItemDTO updateCartItemQuantity(String userId, CartUpdateRequestDTO request) {
        CartItem cartItem = cartItemRepository.findById(request.getCartItemId())
                .orElseThrow(() -> new RuntimeException("장바구니 상품을 찾을 수 없습니다: " + request.getCartItemId()));

        // 권한 확인 (해당 사용자의 장바구니인지)
        Cart cart = cartRepository.findById(cartItem.getCartId())
                .orElseThrow(() -> new RuntimeException("장바구니를 찾을 수 없습니다"));

        if (!cart.getUserId().equals(userId)) {
            throw new RuntimeException("접근 권한이 없습니다");
        }

        if (request.getQuantity() <= 0) {
            cartItemRepository.delete(cartItem);
            log.info("장바구니 상품 삭제: {}", request.getCartItemId());
            return null;
        }

        cartItem.setQuantity(request.getQuantity());
        cartItemRepository.save(cartItem);

        try {
            ProductDTO product = productServiceClient.getProduct(cartItem.getProductId());
            log.info("장바구니 상품 수량 변경: {} -> {}", request.getCartItemId(), request.getQuantity());
            return convertToCartItemDTO(cartItem, product);
        } catch (Exception e) {
            log.error("상품 정보 조회 실패: {}", cartItem.getProductId());
            return convertToCartItemDTOWithoutProduct(cartItem);
        }
    }

    /**
     * 장바구니 상품 삭제
     */
    public void removeCartItem(String userId, String cartItemId) {
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("장바구니 상품을 찾을 수 없습니다: " + cartItemId));

        // 권한 확인
        Cart cart = cartRepository.findById(cartItem.getCartId())
                .orElseThrow(() -> new RuntimeException("장바구니를 찾을 수 없습니다"));

        if (!cart.getUserId().equals(userId)) {
            throw new RuntimeException("접근 권한이 없습니다");
        }

        cartItemRepository.delete(cartItem);
        log.info("장바구니 상품 삭제: {}", cartItemId);
    }

    /**
     * 선택된 장바구니 상품들 삭제
     */
    public void removeCartItems(String userId, List<String> cartItemIds) {
        for (String cartItemId : cartItemIds) {
            removeCartItem(userId, cartItemId);
        }
        log.info("장바구니 상품 일괄 삭제: {} 개", cartItemIds.size());
    }

    /**
     * 장바구니 전체 비우기
     */
    public void clearCart(String userId) {
        Cart cart = cartRepository.findActiveCartByUserId(userId)
                .orElseThrow(() -> new RuntimeException("장바구니를 찾을 수 없습니다"));

        List<CartItem> cartItems = cartItemRepository.findByCartIdOrderByUpdatedDateDesc(cart.getCartId());
        cartItemRepository.deleteAll(cartItems);
        log.info("장바구니 전체 비우기 완료: {}", userId);
    }

    /**
     * CartItem을 CartItemDTO로 변환 (상품 정보 포함)
     */
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

    /**
     * 상품 정보 없이 CartItemDTO 생성 (fallback)
     */
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

    /**
     * 장바구니 총액 계산
     */
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

        // 배송비 계산 (4만원 이상 무료배송)
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
}