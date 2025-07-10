package org.kosa.commerceservice.service.product;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.kosa.commerceservice.dto.cart.GuestCartItemDTO;
import org.kosa.commerceservice.dto.product.ProductDTO;
import org.kosa.commerceservice.dto.product.ProductDetailDTO;
import org.kosa.commerceservice.dto.productImage.ProductImageDto;
import org.kosa.commerceservice.entity.product.Product;
import org.kosa.commerceservice.repository.product.ProductRepository;
import org.kosa.commerceservice.service.category.CategoryService;
import org.kosa.commerceservice.service.productImage.ProductImageService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryService categoryService;
    private final ProductImageService productImageService;
    private final RedisTemplate<String, Object> redisTemplate;

    @CircuitBreaker(name = "productService", fallbackMethod = "getProductByIdFallback")
    @Retry(name = "productService")
    @TimeLimiter(name = "productService")
    @Cacheable(value = "productDetail", key = "#productId", unless = "#result == null")
    public CompletableFuture<ProductDTO> getProductByIdAsync(Integer productId) {
        return CompletableFuture.supplyAsync(() -> getProductById(productId));
    }

    @Cacheable(value = "productDetail", key = "#productId", unless = "#result == null")
    public ProductDTO getProductById(Integer productId) {
        log.info("DB에서 상품 상세 조회: {}", productId);

        Optional<Product> productOpt = productRepository.findByProductIdAndProductStatus(productId, "판매중");
        if (productOpt.isPresent()) {
            ProductDTO dto = convertToDto(productOpt.get());

            try {
                attachImagesToProduct(dto);
            } catch (Exception e) {
                log.warn("이미지 첨부 실패, 기본 이미지 사용: {}", e.getMessage());
            }

            log.info("상품 상세 조회 완료 (캐시 저장): {}", productId);
            return dto;
        }

        log.warn("상품을 찾을 수 없음: {}", productId);
        throw new RuntimeException("상품을 찾을 수 없습니다: " + productId);
    }

    @CircuitBreaker(name = "productService", fallbackMethod = "getRelatedProductsFallback")
    @Retry(name = "productService")
    @Cacheable(value = "relatedProducts", key = "#productId + ':' + #limit")
    public List<ProductDTO> getRelatedProducts(Integer productId, int limit) {
        log.info("관련 상품 조회: {}, limit: {}", productId, limit);

        Optional<Product> currentProductOpt = productRepository.findByProductIdAndProductStatus(productId, "판매중");
        if (!currentProductOpt.isPresent()) {
            return new ArrayList<>();
        }

        Product currentProduct = currentProductOpt.get();
        Integer categoryId = currentProduct.getCategoryId();

        Pageable pageable = PageRequest.of(0, limit);
        List<Product> relatedProducts = productRepository.findByCategoryIdAndProductStatusAndProductIdNot(
                categoryId, "판매중", productId, pageable);

        List<ProductDTO> result = relatedProducts.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        log.info("관련 상품 조회 완료: {}개", result.size());
        return result;
    }

    @CircuitBreaker(name = "productService", fallbackMethod = "getAllProductsFallback")
    @Retry(name = "productService")
    @Cacheable(value = "productList", key = "'all:' + #limit")
    public List<ProductDTO> getAllProducts(int limit) {
        try {
            log.info("전체 상품 조회: limit {}", limit);

            Pageable pageable = PageRequest.of(0, limit);
            List<Product> products = productRepository.findAllActiveProducts(pageable);

            List<ProductDTO> result = convertToDtoList(products);
            log.info("전체 상품 조회 완료: {}개", result.size());
            return result;
        } catch (Exception e) {
            log.error("전체 상품 조회 실패:", e);
            throw new RuntimeException("전체 상품 조회 중 오류가 발생했습니다.");
        }
    }

    @CircuitBreaker(name = "productService", fallbackMethod = "getProductsByCategoryFallback")
    @Retry(name = "productService")
    @Cacheable(value = "productsByCategory", key = "#categoryId + ':' + #limit")
    public List<ProductDTO> getProductsByCategory(Integer categoryId, int limit) {
        try {
            log.info("카테고리별 상품 조회 - categoryId: {}, limit: {}", categoryId, limit);

            Pageable pageable = PageRequest.of(0, limit);
            List<Product> products = new ArrayList<>();

            if (isMainCategory(categoryId)) {
                log.info("메인 카테고리 감지: {}", categoryId);

                try {
                    List<Integer> childrenCategoryIds = getCachedChildrenCategoryIds(categoryId);
                    log.info("하위 카테고리 ID들: {}", childrenCategoryIds);

                    if (childrenCategoryIds != null && !childrenCategoryIds.isEmpty()) {
                        List<Integer> allCategoryIds = new ArrayList<>();
                        allCategoryIds.add(categoryId);
                        allCategoryIds.addAll(childrenCategoryIds);

                        log.info("통합 카테고리 조회 대상: {}", allCategoryIds);
                        products = productRepository.findByMultipleCategoriesActive(allCategoryIds, pageable);
                    } else {
                        log.info("하위 카테고리 없음. 메인 카테고리만 조회: {}", categoryId);
                        products = productRepository.findByCategoryIdActive(categoryId, pageable);
                    }
                } catch (Exception e) {
                    log.error("카테고리 서비스 호출 실패. 단일 카테고리 조회: {}", categoryId, e);
                    products = productRepository.findByCategoryIdActive(categoryId, pageable);
                }
            } else {
                log.info("하위/일반 카테고리 조회: {}", categoryId);
                products = productRepository.findByCategoryIdActive(categoryId, pageable);
            }

            if (!products.isEmpty()) {
                log.info("DB에서 조회된 상품 {}개:", products.size());
                Map<Integer, Long> categoryCount = products.stream()
                        .collect(Collectors.groupingBy(Product::getCategoryId, Collectors.counting()));

                categoryCount.forEach((catId, count) ->
                        log.info("  - 카테고리 {}: {}개", catId, count)
                );

                if (!isMainCategory(categoryId)) {
                    long wrongCategoryCount = products.stream()
                            .filter(p -> !p.getCategoryId().equals(categoryId))
                            .count();

                    if (wrongCategoryCount > 0) {
                        log.error("심각한 문제: 카테고리 {} 요청했는데 다른 카테고리 상품 {}개가 조회됨!",
                                categoryId, wrongCategoryCount);

                        products.stream()
                                .filter(p -> !p.getCategoryId().equals(categoryId))
                                .limit(5)
                                .forEach(p -> log.error("  - 잘못된 상품: ID={}, 이름={}, 카테고리={}",
                                        p.getProductId(), p.getName(), p.getCategoryId()));
                    }
                }
            } else {
                log.warn("카테고리 {}에서 조회된 상품이 없습니다", categoryId);
            }

            List<ProductDTO> result = convertToDtoList(products);
            log.info("카테고리별 상품 조회 완료: {}개", result.size());
            return result;

        } catch (Exception e) {
            log.error("카테고리별 상품 조회 실패 - categoryId: {}", categoryId, e);
            throw new RuntimeException("카테고리별 상품 조회 중 오류가 발생했습니다.");
        }
    }

    @CircuitBreaker(name = "productService", fallbackMethod = "getProductsByHostFallback")
    @Retry(name = "productService")
    @Cacheable(value = "productsByHost", key = "#hostId + ':' + #limit")
    public List<ProductDTO> getProductsByHost(Long hostId, int limit) {
        try {
            log.info("HOST별 상품 조회: hostId {}, limit: {}", hostId, limit);

            Pageable pageable = PageRequest.of(0, limit);
            List<Product> products = productRepository.findByHostIdAndProductStatus(hostId, "판매중", pageable);

            List<ProductDTO> result = convertToDtoList(products);
            log.info("HOST별 상품 조회 완료: {}개", result.size());
            return result;
        } catch (Exception e) {
            log.error("HOST별 상품 조회 실패:", e);
            throw new RuntimeException("HOST별 상품 조회 중 오류가 발생했습니다.");
        }
    }

    @CircuitBreaker(name = "productService", fallbackMethod = "getProductsInStockFallback")
    @Retry(name = "productService")
    @Cacheable(value = "productsInStock", key = "'inStock:' + #limit")
    public List<ProductDTO> getProductsInStock(int limit) {
        try {
            log.info("재고 있는 상품 조회: limit {}", limit);

            Pageable pageable = PageRequest.of(0, limit);
            List<Product> products = productRepository.findByStockGreaterThanZeroAndProductStatus("판매중", pageable);

            List<ProductDTO> result = convertToDtoList(products);
            log.info("재고 있는 상품 조회 완료: {}개", result.size());
            return result;
        } catch (Exception e) {
            log.error("재고 있는 상품 조회 실패:", e);
            throw new RuntimeException("재고 있는 상품 조회 중 오류가 발생했습니다.");
        }
    }

    @CircuitBreaker(name = "productService", fallbackMethod = "getDiscountedProductsFallback")
    @Retry(name = "productService")
    @Cacheable(value = "discountedProducts", key = "'discount:' + #limit")
    public List<ProductDTO> getDiscountedProducts(int limit) {
        try {
            log.info("할인 상품 조회: limit {}", limit);

            Pageable pageable = PageRequest.of(0, limit);
            List<Product> products = productRepository.findDiscountedProducts("판매중", pageable);

            List<ProductDTO> result = convertToDtoList(products);
            log.info("할인 상품 조회 완료: {}개", result.size());
            return result;
        } catch (Exception e) {
            log.error("할인 상품 조회 실패:", e);
            throw new RuntimeException("할인 상품 조회 중 오류가 발생했습니다.");
        }
    }

    @CircuitBreaker(name = "productService", fallbackMethod = "getPopularProductsFallback")
    @Retry(name = "productService")
    @Cacheable(value = "popularProducts", key = "'popular:' + #limit")
    public List<ProductDTO> getPopularProducts(int limit) {
        try {
            log.info("인기 상품 조회: limit {}", limit);

            Pageable pageable = PageRequest.of(0, limit);
            List<Product> products = productRepository.findPopularProducts("판매중", pageable);

            List<ProductDTO> result = convertToDtoList(products);
            log.info("인기 상품 조회 완료: {}개", result.size());
            return result;
        } catch (Exception e) {
            log.error("인기 상품 조회 실패:", e);
            throw new RuntimeException("인기 상품 조회 중 오류가 발생했습니다.");
        }
    }

    @CircuitBreaker(name = "categoryService", fallbackMethod = "getCachedChildrenCategoryIdsFallback")
    @Retry(name = "categoryService")
    private List<Integer> getCachedChildrenCategoryIds(Integer categoryId) {
        String cacheKey = "category:children:" + categoryId;

        try {
            @SuppressWarnings("unchecked")
            List<Integer> cachedIds = (List<Integer>) redisTemplate.opsForValue().get(cacheKey);

            if (cachedIds != null) {
                log.info("캐시에서 하위 카테고리 ID 조회: {}", cachedIds);
                return cachedIds;
            }

            List<Integer> childrenIds = categoryService.getAllChildrenIds(categoryId);

            if (childrenIds != null) {
                redisTemplate.opsForValue().set(cacheKey, childrenIds, 30, TimeUnit.MINUTES);
                log.info("하위 카테고리 ID 캐시 저장: {}", childrenIds);
            }

            return childrenIds;

        } catch (Exception e) {
            log.error("하위 카테고리 ID 조회 실패: {}", categoryId, e);
            throw new RuntimeException("하위 카테고리 조회 중 오류가 발생했습니다.");
        }
    }

    @Transactional
    public void increaseViewCount(Integer productId) {
        Optional<Product> productOpt = productRepository.findByProductIdAndProductStatus(productId, "판매중");
        productOpt.ifPresent(product -> {
            log.debug("조회수 증가: {}", productId);
        });
    }

    public Long getProductCountByCategory(Integer categoryId) {
        return productRepository.countByCategoryActive(categoryId);
    }

    public Long getTotalActiveProductCount() {
        return productRepository.countAllActiveProducts();
    }

    public Map<Integer, Long> getProductCountsByAllCategories() {
        List<Object[]> results = productRepository.getProductCountsByCategory();
        return results.stream().collect(Collectors.toMap(
                result -> (Integer) result[0],
                result -> ((Number) result[1]).longValue()
        ));
    }

    public List<ProductDetailDTO> getProductsForGuestCart(List<GuestCartItemDTO> cartItems) {
        List<Integer> productIds = cartItems.stream()
                .map(GuestCartItemDTO::getProductId)
                .distinct()
                .collect(Collectors.toList());

        if (productIds.isEmpty()) return new ArrayList<>();

        List<Product> products = productRepository.findByProductIdInAndProductStatus(productIds, "판매중");
        return products.stream().map(this::convertToProductDetailDTO).collect(Collectors.toList());
    }

    // Fallback Methods
    public CompletableFuture<ProductDTO> getProductByIdFallback(Integer productId, Exception ex) {
        log.error("상품 조회 서킷브레이커 동작 - productId: {}, error: {}", productId, ex.getMessage());

        ProductDTO fallbackProduct = ProductDTO.builder()
                .productId(productId)
                .name("일시적으로 조회할 수 없는 상품")
                .price(0)
                .productStatus("일시중단")
                .image("https://via.placeholder.com/300x200?text=No+Image")
                .build();

        return CompletableFuture.completedFuture(fallbackProduct);
    }

    public List<ProductDTO> getRelatedProductsFallback(Integer productId, int limit, Exception ex) {
        log.error("관련 상품 조회 서킷브레이커 동작 - productId: {}, error: {}", productId, ex.getMessage());

        return List.of(ProductDTO.builder()
                .productId(0)
                .name("관련 상품을 일시적으로 불러올 수 없습니다")
                .price(0)
                .productStatus("서비스 점검중")
                .build());
    }

    public List<ProductDTO> getAllProductsFallback(int limit, Exception ex) {
        log.error("전체 상품 조회 서킷브레이커 동작 - limit: {}, error: {}", limit, ex.getMessage());

        return List.of(ProductDTO.builder()
                .productId(0)
                .name("상품을 일시적으로 불러올 수 없습니다")
                .price(0)
                .productStatus("서비스 점검중")
                .build());
    }

    public List<ProductDTO> getProductsByCategoryFallback(Integer categoryId, int limit, Exception ex) {
        log.error("카테고리별 상품 조회 서킷브레이커 동작 - categoryId: {}, error: {}", categoryId, ex.getMessage());

        return List.of(ProductDTO.builder()
                .productId(0)
                .name("카테고리 상품을 일시적으로 불러올 수 없습니다")
                .price(0)
                .productStatus("서비스 점검중")
                .build());
    }

    public List<ProductDTO> getProductsByHostFallback(Long hostId, int limit, Exception ex) {
        log.error("HOST별 상품 조회 서킷브레이커 동작 - hostId: {}, error: {}", hostId, ex.getMessage());

        return List.of(ProductDTO.builder()
                .productId(0)
                .name("HOST 상품을 일시적으로 불러올 수 없습니다")
                .price(0)
                .productStatus("서비스 점검중")
                .build());
    }

    public List<ProductDTO> getProductsInStockFallback(int limit, Exception ex) {
        log.error("재고 상품 조회 서킷브레이커 동작 - limit: {}, error: {}", limit, ex.getMessage());
        return new ArrayList<>();
    }

    public List<ProductDTO> getDiscountedProductsFallback(int limit, Exception ex) {
        log.error("할인 상품 조회 서킷브레이커 동작 - limit: {}, error: {}", limit, ex.getMessage());
        return new ArrayList<>();
    }

    public List<ProductDTO> getPopularProductsFallback(int limit, Exception ex) {
        log.error("인기 상품 조회 서킷브레이커 동작 - limit: {}, error: {}", limit, ex.getMessage());
        return new ArrayList<>();
    }

    public List<Integer> getCachedChildrenCategoryIdsFallback(Integer categoryId, Exception ex) {
        log.error("하위 카테고리 조회 서킷브레이커 동작 - categoryId: {}, error: {}", categoryId, ex.getMessage());
        return new ArrayList<>();
    }

    // Image processing methods
    public void attachImagesToProduct(ProductDTO product) {
        try {
            Integer productId = product.getProductId();

            List<ProductImageDto> images = productImageService.getProductImages(productId);
            product.setProductImages(images);
            product.setImages(images.stream()
                    .map(ProductImageDto::getImageUrl)
                    .collect(Collectors.toList()));

            ProductImageDto mainImage = productImageService.getMainImageDto(productId);
            if (mainImage != null) {
                product.setMainImage(mainImage.getImageUrl());
                product.setImage(mainImage.getImageUrl());
            }
        } catch (Exception e) {
            log.warn("이미지 조회 실패: {}", product.getProductId(), e);
            product.setProductImages(new ArrayList<>());
            product.setImages(new ArrayList<>());
        }
    }

    public void attachMainImagesToProducts(List<ProductDTO> products) {
        if (products == null || products.isEmpty()) return;

        List<Integer> productIds = products.stream()
                .map(ProductDTO::getProductId)
                .collect(Collectors.toList());

        Map<String, ProductImageDto> mainImages = productImageService.getMainImages(productIds);

        products.forEach(product -> {
            ProductImageDto mainImage = mainImages.get(product.getProductId().toString());
            if (mainImage != null) {
                String imageUrl = mainImage.getImageUrl();
                product.setMainImage(imageUrl);
                product.setImage(imageUrl);
                product.setImages(Collections.singletonList(imageUrl));
            } else {
                setDefaultImage(product);
            }
        });
    }

    // Utility methods
    private boolean isMainCategory(Integer categoryId) {
        if (categoryId == null) {
            return false;
        }
        return categoryId < 100;
    }

    private ProductDTO convertToDto(Product product) {
        Integer discount = calculateDiscountRate(product.getPrice(), product.getSalePrice());
        return ProductDTO.builder()
                .productId(product.getProductId())
                .categoryId(product.getCategoryId())
                .name(product.getName())
                .price(product.getPrice())
                .salePrice(product.getSalePrice())
                .originalPrice(product.getPrice())
                .productDescription(product.getProductDescription())
                .productShortDescription(product.getProductShortDescription())
                .productStatus(product.getProductStatus())
                .productRating(product.getProductRating())
                .productReviewCount(product.getProductReviewCount())
                .createdDate(product.getCreatedDate())
                .updatedDate(product.getUpdatedDate())
                .mainImage(product.getMainImage())
                .image(getProductImageUrl(product))
                .viewCount(product.getViewCount())
                .stock(product.getStock())
                .hostId(product.getHostId())
                .displayYn(product.getDisplayYn())
                .discount(discount)
                .isLive(false)
                .viewers(null)
                .subtitle(product.getProductShortDescription())
                .brand(extractBrandFromName(product.getName()))
                .origin("상품설명/상세정보 참조")
                .deliveryInfo("냉동 (종이포장)")
                .packaging("1팩")
                .weight("상품설명 참조")
                .ingredients("상품설명 참조")
                .allergyInfo("상품설명 참조")
                .images(createImageList(product.getMainImage()))
                .detailImages(new ArrayList<>())
                .discountRate(discount)
                .discountPrice(product.getSalePrice())
                .averageRating(product.getProductRating() != null ? product.getProductRating().doubleValue() : 4.5)
                .reviewCount(product.getProductReviewCount() != null ? product.getProductReviewCount() : 0)
                .stockQuantity(product.getStock() != null ? product.getStock().longValue() : 0L)
                .categoryName("카테고리")
                .build();
    }

    private ProductDetailDTO convertToProductDetailDTO(Product product) {
        return ProductDetailDTO.builder()
                .productId(product.getProductId())
                .name(product.getName())
                .price(product.getPrice())
                .salePrice(product.getSalePrice())
                .mainImage(product.getMainImage())
                .description(product.getProductDescription())
                .stock(product.getStock())
                .hostId(product.getHostId())
                .displayYn(product.getDisplayYn())
                .categoryId(product.getCategoryId())
                .build();
    }

    private List<ProductDTO> convertToDtoList(List<Product> products) {
        return products == null ? new ArrayList<>() :
                products.stream()
                        .map(this::convertToDto)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
    }

    private void setDefaultImage(ProductDTO product) {
        String defaultImageUrl = getDefaultImageUrl();
        product.setMainImage(defaultImageUrl);
        product.setImage(defaultImageUrl);
        product.setImages(Collections.singletonList(defaultImageUrl));
    }

    private String getDefaultImageUrl() {
        return "data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMzAwIiBoZWlnaHQ9IjIwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iMTAwJSIgaGVpZ2h0PSIxMDAlIiBmaWxsPSIjZGRkIi8+PHRleHQgeD0iNTAlIiB5PSI1MCUiIGZvbnQtZmFtaWx5PSJBcmlhbCIgZm9udC1zaXplPSIxNCIgZmlsbD0iIzk5OSIgdGV4dC1hbmNob3I9Im1pZGRsZSIgZHk9Ii4zZW0iPk5vIEltYWdlPC90ZXh0Pjwvc3ZnPg==";
    }

    private String getProductImageUrl(Product product) {
        if (product.getMainImage() != null && !product.getMainImage().trim().isEmpty()) {
            return product.getMainImage();
        }
        String productName = product.getName() != null ?
                product.getName().substring(0, Math.min(product.getName().length(), 10)) :
                "No+Image";
        return "https://via.placeholder.com/300x200?text=" + productName;
    }

    private Integer calculateDiscountRate(Integer originalPrice, Integer salePrice) {
        if (salePrice == null || originalPrice == null || originalPrice == 0) {
            return null;
        }
        return (int) Math.round(((double) (originalPrice - salePrice) / originalPrice) * 100);
    }

    private String extractBrandFromName(String productName) {
        if (productName != null) {
            if (productName.contains("[") && productName.contains("]")) {
                int start = productName.indexOf("[") + 1;
                int end = productName.indexOf("]");
                if (start < end) {
                    return productName.substring(start, end);
                }
            }
            if (productName.contains("(") && productName.contains(")")) {
                int start = productName.indexOf("(") + 1;
                int end = productName.indexOf(")");
                if (start < end) {
                    return productName.substring(start, end);
                }
            }
        }
        return "브랜드명";
    }

    private List<String> createImageList(String mainImage) {
        String imageUrl = mainImage != null && !mainImage.trim().isEmpty() ?
                mainImage :
                "https://via.placeholder.com/600x600?text=상품+이미지";
        return Collections.singletonList(imageUrl);
    }
}