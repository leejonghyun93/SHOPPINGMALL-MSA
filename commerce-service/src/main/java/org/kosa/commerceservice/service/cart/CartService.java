package org.kosa.commerceservice.service.cart;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.kosa.commerceservice.client.ProductServiceClient;
import org.kosa.commerceservice.dto.cart.CartDTO;
import org.kosa.commerceservice.dto.cart.CartItemDTO;
import org.kosa.commerceservice.dto.cart.CartRequestDTO;
import org.kosa.commerceservice.dto.cart.CartUpdateRequestDTO;
import org.kosa.commerceservice.dto.product.ProductDTO;
import org.kosa.commerceservice.entity.cart.Cart;
import org.kosa.commerceservice.entity.cart.CartItem;
import org.kosa.commerceservice.repository.cart.CartItemRepository;
import org.kosa.commerceservice.repository.cart.CartRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductServiceClient productServiceClient;

    @CircuitBreaker(name = "cartService", fallbackMethod = "addToCartFallback")
    @Retry(name = "cartService")
    @TimeLimiter(name = "cartService")
    @Transactional
    public CompletableFuture<CartItemDTO> addToCartAsync(String userId, CartRequestDTO request) {
        return CompletableFuture.supplyAsync(() -> addToCart(userId, request));
    }

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

    @CircuitBreaker(name = "productService", fallbackMethod = "getCartFallback")
    @Retry(name = "productService")
    @Transactional(readOnly = true)
    public CartDTO getCart(String userId) {
        Cart cart = cartRepository.findActiveCartByUserId(userId).orElse(null);

        if (cart == null) {
            return CartDTO.builder()
                    .userId(userId)
                    .cartItems(List.of())
                    .totalItems(0)
                    .totalPrice(0)
                    .totalDiscountPrice(0)
                    .finalPrice(0)
                    .deliveryFee(0)
                    .build();
        }

        List<CartItem> cartItems = cartItemRepository.findByCartId(cart.getCartId());

        List<CartItemDTO> cartItemDTOs = cartItems.stream()
                .map(item -> {
                    try {
                        ProductDTO product = getProductWithCircuitBreaker(item.getProductId());
                        return convertToCartItemDTO(item, product);
                    } catch (Exception e) {
                        log.warn("상품 정보 조회 실패, 기본값 사용: productId={}", item.getProductId());
                        return convertToCartItemDTOWithoutProduct(item);
                    }
                })
                .collect(Collectors.toList());

        CartDTO result = calculateCartTotals(cart, cartItemDTOs);
        return result;
    }

    @CircuitBreaker(name = "productService", fallbackMethod = "getProductFallback")
    @Retry(name = "productService")
    private ProductDTO getProductWithCircuitBreaker(Integer productId) {
        return productServiceClient.getProduct(productId);
    }

    @Transactional
    public void updateCartItemQuantity(String userId, String cartItemId, Integer newQuantity) {
        try {
            log.info("장바구니 수량 변경 - userId: {}, cartItemId: {}, newQuantity: {}",
                    userId, cartItemId, newQuantity);

            Optional<CartItem> cartItemOpt = cartItemRepository.findById(cartItemId);

            if (cartItemOpt.isEmpty()) {
                log.warn("cartItemId로 조회 실패, 대안 조회 시도");

                List<CartItem> userCartItems = cartItemRepository.findByUserId(userId);
                log.info("사용자 장바구니 아이템 수: {}", userCartItems.size());

                for (CartItem item : userCartItems) {
                    log.debug("- cartItemId: {}, productId: {}",
                            item.getCartItemId(), item.getProductId());
                }

                cartItemOpt = userCartItems.stream()
                        .filter(item -> cartItemId.equals(item.getCartItemId()))
                        .findFirst();
            }

            if (cartItemOpt.isEmpty()) {
                log.error("장바구니 아이템 조회 완전 실패 - cartItemId: {}", cartItemId);
                throw new IllegalArgumentException("해당 장바구니 상품을 찾을 수 없습니다: " + cartItemId);
            }

            CartItem cartItem = cartItemOpt.get();

            Cart cart = cartRepository.findById(cartItem.getCartId())
                    .orElseThrow(() -> new IllegalArgumentException("장바구니를 찾을 수 없습니다."));

            if (!cart.getUserId().equals(userId)) {
                log.error("권한 없음: cart.userId={}, request.userId={}",
                        cart.getUserId(), userId);
                throw new IllegalArgumentException("접근 권한이 없습니다.");
            }

            Integer oldQuantity = cartItem.getQuantity();
            cartItem.setQuantity(newQuantity);

            CartItem savedItem = cartItemRepository.save(cartItem);

            log.info("수량 변경 완료 - cartItemId: {}, {}개 → {}개",
                    cartItemId, oldQuantity, savedItem.getQuantity());

        } catch (Exception e) {
            log.error("수량 변경 실패: userId={}, cartItemId={}, error={}",
                    userId, cartItemId, e.getMessage(), e);
            throw new RuntimeException("수량 변경에 실패했습니다: " + e.getMessage(), e);
        }
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

    @Transactional(readOnly = true)
    public int getCartItemCount(String userId) {
        try {
            log.info("사용자 장바구니 개수 조회: userId={}", userId);

            Optional<Cart> cartOpt = cartRepository.findActiveCartByUserId(userId);

            if (!cartOpt.isPresent()) {
                log.info("활성 장바구니 없음: userId={}", userId);
                return 0;
            }

            Cart cart = cartOpt.get();

            Integer totalQuantity = cartItemRepository.sumQuantityByCartId(cart.getCartId());

            int count = (totalQuantity != null) ? totalQuantity : 0;
            log.info("장바구니 개수 조회 결과: userId={}, cartId={}, count={}",
                    userId, cart.getCartId(), count);

            return count;

        } catch (Exception e) {
            log.error("장바구니 개수 조회 실패: userId={}, error={}", userId, e.getMessage());
            return 0;
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

    @Transactional
    public void removeCartItems(String userId, List<String> cartItemIds) {
        try {
            log.info("다중 장바구니 아이템 제거 - userId: {}, cartItemIds: {}", userId, cartItemIds);

            if (cartItemIds == null || cartItemIds.isEmpty()) {
                throw new IllegalArgumentException("삭제할 상품이 선택되지 않았습니다.");
            }

            Optional<Cart> cartOpt = cartRepository.findByUserIdAndStatus(userId, "ACTIVE");

            if (cartOpt.isEmpty()) {
                throw new IllegalArgumentException("활성 장바구니를 찾을 수 없습니다.");
            }

            Cart cart = cartOpt.get();
            String cartId = cart.getCartId();

            for (String cartItemId : cartItemIds) {
                try {
                    removeCartItem(userId, cartItemId);
                } catch (Exception e) {
                    log.warn("개별 아이템 삭제 실패: cartItemId={}, error={}", cartItemId, e.getMessage());
                }
            }

            log.info("다중 장바구니 아이템 제거 시도 완료");

        } catch (Exception e) {
            log.error("다중 장바구니 아이템 제거 실패: userId={}, error={}", userId, e.getMessage(), e);
            throw new RuntimeException("장바구니 아이템 제거에 실패했습니다: " + e.getMessage(), e);
        }
    }

    public void clearCart(String userId) {
        Cart cart = cartRepository.findActiveCartByUserId(userId)
                .orElseThrow(() -> new RuntimeException("장바구니를 찾을 수 없습니다"));

        List<CartItem> cartItems = cartItemRepository.findByCartIdOrderByUpdatedDateDesc(cart.getCartId());
        cartItemRepository.deleteAll(cartItems);
    }

    @CircuitBreaker(name = "cartService", fallbackMethod = "removePurchasedItemsFallback")
    @Retry(name = "cartService")
    @Transactional
    public int removePurchasedItems(String userId, List<Integer> productIds) {
        try {
            log.info("구매 완료 상품 제거 시작 - userId: {}, productIds: {}", userId, productIds);

            if (productIds == null || productIds.isEmpty()) {
                log.warn("제거할 상품 ID 목록이 비어있음");
                return 0;
            }

            Optional<Cart> cartOpt = cartRepository.findByUserIdAndStatus(userId, "ACTIVE");

            if (cartOpt.isEmpty()) {
                cartOpt = cartRepository.findByUserId(userId);
                if (cartOpt.isEmpty()) {
                    log.info("사용자의 장바구니가 없음 - userId: {}", userId);
                    return 0;
                }
            }

            Cart cart = cartOpt.get();
            String cartId = cart.getCartId();

            List<CartItem> cartItems = cartItemRepository.findByCartId(cartId);

            List<CartItem> itemsToRemove = cartItems.stream()
                    .filter(item -> productIds.contains(item.getProductId()))
                    .collect(Collectors.toList());

            if (itemsToRemove.isEmpty()) {
                log.info("장바구니에서 제거할 상품이 없음 - userId: {}", userId);
                return 0;
            }

            log.info("제거할 장바구니 아이템들: {}",
                    itemsToRemove.stream()
                            .map(item -> "productId:" + item.getProductId() + ",cartItemId:" + item.getCartItemId())
                            .collect(Collectors.toList()));

            int removedCount = 0;
            for (CartItem item : itemsToRemove) {
                try {
                    if (cart.getCartItems() != null) {
                        cart.getCartItems().remove(item);
                    }

                    cartItemRepository.delete(item);
                    removedCount++;

                    log.debug("삭제 완료: cartItemId={}, productId={}",
                            item.getCartItemId(), item.getProductId());

                } catch (Exception e) {
                    log.warn("개별 상품 삭제 실패: cartItemId={}, productId={}, error={}",
                            item.getCartItemId(), item.getProductId(), e.getMessage());
                }
            }

            cart.setUpdatedDate(LocalDateTime.now());
            cartRepository.save(cart);

            log.info("{}개 상품이 장바구니에서 제거됨 - userId: {}", removedCount, userId);

            return removedCount;

        } catch (Exception e) {
            log.error("구매 완료 상품 제거 중 오류: userId={}, error={}", userId, e.getMessage(), e);
            throw new RuntimeException("구매 완료 상품 제거에 실패했습니다: " + e.getMessage(), e);
        }
    }

    // Fallback Methods
    public CompletableFuture<CartItemDTO> addToCartFallback(String userId, CartRequestDTO request, Exception ex) {
        log.error("장바구니 추가 서킷브레이커 동작 - userId: {}, error: {}", userId, ex.getMessage());

        CartItemDTO fallbackItem = CartItemDTO.builder()
                .productId(request.getProductId())
                .quantity(request.getQuantity())
                .productName("일시적으로 처리할 수 없는 상품")
                .build();

        return CompletableFuture.completedFuture(fallbackItem);
    }

    public CartDTO getCartFallback(String userId, Exception ex) {
        log.error("장바구니 조회 서킷브레이커 동작 - userId: {}, error: {}", userId, ex.getMessage());

        return CartDTO.builder()
                .userId(userId)
                .cartItems(List.of())
                .totalItems(0)
                .totalPrice(0)
                .totalDiscountPrice(0)
                .finalPrice(0)
                .deliveryFee(0)
                .build();
    }

    public ProductDTO getProductFallback(Integer productId, Exception ex) {
        log.error("상품 정보 조회 서킷브레이커 동작 - productId: {}, error: {}", productId, ex.getMessage());

        return ProductDTO.builder()
                .productId(productId)
                .name("일시적으로 조회할 수 없는 상품")
                .price(0)
                .salePrice(0)
                .productStatus("일시중단")
                .build();
    }

    public int removePurchasedItemsFallback(String userId, List<Integer> productIds, Exception ex) {
        log.error("구매 완료 상품 제거 서킷브레이커 동작 - userId: {}, error: {}", userId, ex.getMessage());
        return 0;
    }

    // Helper Methods
    private CartItemDTO convertToCartItemDTO(CartItem cartItem, ProductDTO product) {
        Integer salePrice = product.getSalePrice() != null ? product.getSalePrice() : product.getPrice();
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
                .productPrice(0)
                .salePrice(0)
                .discountRate(0)
                .category("normal")
                .deliveryType("dawn")
                .itemTotalPrice(0)
                .itemTotalSalePrice(0)
                .build();
    }

    private CartDTO calculateCartTotals(Cart cart, List<CartItemDTO> cartItems) {
        Integer totalItems = cartItems.stream()
                .mapToInt(CartItemDTO::getQuantity)
                .sum();

        Integer totalPrice = cartItems.stream()
                .mapToInt(CartItemDTO::getItemTotalPrice)
                .sum();

        Integer totalSalePrice = cartItems.stream()
                .mapToInt(CartItemDTO::getItemTotalSalePrice)
                .sum();

        Integer totalDiscountPrice = totalPrice - totalSalePrice;

        Integer deliveryFee = totalSalePrice >= 40000 ? 0 : 3000;

        Integer finalPrice = totalSalePrice + deliveryFee;

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