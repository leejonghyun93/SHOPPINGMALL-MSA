package org.kosa.cartservice.service;

import lombok.RequiredArgsConstructor;
import org.kosa.cartservice.client.ProductServiceClient;
import org.kosa.cartservice.dto.*;
import org.kosa.cartservice.entity.Cart;
import org.kosa.cartservice.entity.CartItem;
import org.kosa.cartservice.mapper.CartItemRepository;
import org.kosa.cartservice.mapper.CartRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
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
}