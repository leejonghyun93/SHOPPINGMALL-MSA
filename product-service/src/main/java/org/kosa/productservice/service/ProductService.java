package org.kosa.productservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kosa.productservice.client.CategoryServiceClient;
import org.kosa.productservice.client.ImageServiceClient;
import org.kosa.productservice.dto.*;
import org.kosa.productservice.mapper.ProductRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryServiceClient categoryServiceClient;
    private final ImageServiceClient imageServiceClient;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 🔥 상품 상세 조회 - Redis 캐시 적용
     */
    @Cacheable(value = "productDetail", key = "#productId", unless = "#result == null")
    public ProductDto getProductById(String productId) {
        log.info("🔍 DB에서 상품 상세 조회: {}", productId);

        Optional<Product> productOpt = productRepository.findByProductIdAndProductStatus(productId, "ACTIVE");
        if (productOpt.isPresent()) {
            ProductDto dto = convertToDto(productOpt.get());

            // 🔥 try-catch로 이미지 서비스 오류 방지
            try {
                attachImagesToProduct(dto);
            } catch (Exception e) {
                log.warn("이미지 첨부 실패, 기본 이미지 사용: {}", e.getMessage());
            }

            log.info("✅ 상품 상세 조회 완료 (캐시 저장): {}", productId);
            return dto;
        }

        log.warn("⚠️ 상품을 찾을 수 없음: {}", productId);
        throw new RuntimeException("상품을 찾을 수 없습니다: " + productId);
    }

    /**
     * 🔥 관련 상품 조회 - Redis 캐시 적용
     */
    @Cacheable(value = "relatedProducts", key = "#productId + ':' + #limit")
    public List<ProductDto> getRelatedProducts(String productId, int limit) {
        log.info("🔍 DB에서 관련 상품 조회: {}, limit: {}", productId, limit);

        Optional<Product> currentProductOpt = productRepository.findByProductIdAndProductStatus(productId, "ACTIVE");
        if (!currentProductOpt.isPresent()) {
            return new ArrayList<>();
        }

        Product currentProduct = currentProductOpt.get();
        String categoryId = currentProduct.getCategoryId();

        Pageable pageable = PageRequest.of(0, limit);
        List<Product> relatedProducts = productRepository.findByCategoryIdAndProductStatusAndProductIdNot(
                categoryId, "ACTIVE", productId, pageable);

        List<ProductDto> result = relatedProducts.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        log.info("✅ 관련 상품 조회 완료 (캐시 저장): {}개", result.size());
        return result;
    }

    /**
     * 🔥 전체 상품 조회 - Redis 캐시 적용
     */
    @Cacheable(value = "productList", key = "'all:' + #limit")
    public List<ProductDto> getAllProducts(int limit) {
        try {
            log.info("🔍 DB에서 전체 상품 조회: limit {}", limit);

            Pageable pageable = PageRequest.of(0, limit);
            List<Product> products = productRepository.findAllActiveProducts(pageable);

            List<ProductDto> result = convertToDtoList(products);
            log.info("✅ 전체 상품 조회 완료 (캐시 저장): {}개", result.size());
            return result;
        } catch (Exception e) {
            log.error("❌ 전체 상품 조회 실패:", e);
            return new ArrayList<>();
        }
    }

    /**
     * 🔥 카테고리별 상품 조회 - Redis 캐시 적용 (핵심!)
     */
    @Cacheable(value = "productsByCategory", key = "#categoryId + ':' + #limit")
    public List<ProductDto> getProductsByCategory(String categoryId, int limit) {
        try {
            log.info("🔍 DB에서 카테고리별 상품 조회 - categoryId: {}, limit: {}", categoryId, limit);

            // ALL 카테고리인 경우 전체 상품 조회 (별도 캐시)
            if ("ALL".equals(categoryId)) {
                return getAllProducts(limit);
            }

            Pageable pageable = PageRequest.of(0, limit);
            List<Product> products = new ArrayList<>();

            // 메인 카테고리인 경우 하위 카테고리 포함 조회
            if (isMainCategory(categoryId)) {
                log.info("📂 메인 카테고리 감지: {}", categoryId);

                try {
                    // 🔥 하위 카테고리 ID 목록도 캐시에서 조회 시도
                    List<String> childrenCategoryIds = getCachedChildrenCategoryIds(categoryId);

                    if (childrenCategoryIds != null && !childrenCategoryIds.isEmpty()) {
                        List<String> allCategoryIds = new ArrayList<>();
                        allCategoryIds.add(categoryId);
                        allCategoryIds.addAll(childrenCategoryIds);

                        log.info("🎯 통합 카테고리 조회 대상: {}", allCategoryIds);
                        products = productRepository.findByMultipleCategoriesActive(allCategoryIds, pageable);
                    } else {
                        log.warn("⚠️ 하위 카테고리 없음. 메인 카테고리만 조회: {}", categoryId);
                        products = productRepository.findByCategoryIdActive(categoryId, pageable);
                    }
                } catch (Exception e) {
                    log.error("❌ 카테고리 서비스 호출 실패. 패턴 기반 폴백: {}", categoryId, e);
                    products = productRepository.findByParentCategoryActive(categoryId, pageable);
                }
            } else {
                // 하위 카테고리 조회
                log.info("🔍 하위 카테고리 조회: {}", categoryId);
                products = productRepository.findByCategoryIdActive(categoryId, pageable);
            }

            List<ProductDto> result = convertToDtoList(products);
            log.info("✅ 카테고리별 상품 조회 완료 (캐시 저장): {}개", result.size());
            return result;

        } catch (Exception e) {
            log.error("❌ 카테고리별 상품 조회 실패 - categoryId: {}", categoryId, e);
            return new ArrayList<>();
        }
    }

    /**
     * 🔥 이미지 포함 전체 상품 조회 - Redis 캐시 적용
     */
    @Cacheable(value = "productList", key = "'withImages:' + #limit")
    public List<ProductDto> getAllProductsWithImages(int limit) {
        log.info("🔍 DB에서 이미지 포함 전체 상품 조회: limit {}", limit);

        Pageable pageable = PageRequest.of(0, limit);
        List<Product> products = productRepository.findAllActiveProducts(pageable);
        List<ProductDto> result = convertToDtoListWithImages(products);

        log.info("✅ 이미지 포함 전체 상품 조회 완료 (캐시 저장): {}개", result.size());
        return result;
    }

    /**
     * 🔥 하위 카테고리 ID 목록 캐시 조회
     */
    private List<String> getCachedChildrenCategoryIds(String categoryId) {
        String cacheKey = "category:children:" + categoryId;

        try {
            // 캐시에서 먼저 조회
            @SuppressWarnings("unchecked")
            List<String> cachedIds = (List<String>) redisTemplate.opsForValue().get(cacheKey);

            if (cachedIds != null) {
                log.info("🎯 캐시에서 하위 카테고리 ID 조회: {}", cachedIds);
                return cachedIds;
            }

            // 캐시에 없으면 CategoryService에서 조회
            List<String> childrenIds = categoryServiceClient.getChildrenCategoryIds(categoryId);

            if (childrenIds != null) {
                // 캐시에 30분간 저장
                redisTemplate.opsForValue().set(cacheKey, childrenIds, 30, TimeUnit.MINUTES);
                log.info("📂 하위 카테고리 ID 캐시 저장: {}", childrenIds);
            }

            return childrenIds;

        } catch (Exception e) {
            log.error("❌ 하위 카테고리 ID 조회 실패: {}", categoryId, e);
            return null;
        }
    }

    /**
     * 🔥 조회수 증가 (캐시 무효화 없음 - 조회수는 실시간 반영 안해도 됨)
     */
    @Transactional
    public void increaseViewCount(String productId) {
        Optional<Product> productOpt = productRepository.findByProductIdAndProductStatus(productId, "ACTIVE");
        productOpt.ifPresent(product -> {
            // 실제 조회수 증가 로직 구현 시 주석 해제
            // product.setViewCount(product.getViewCount() + 1);
            // productRepository.save(product);

            log.debug("조회수 증가: {}", productId);
        });
    }

    // ===== 기존 메서드들 (변경 없음) =====

    public Long getProductCountByCategory(String categoryId) {
        if ("ALL".equals(categoryId)) return productRepository.countAllActiveProducts();
        return productRepository.countByCategoryActive(categoryId);
    }

    public Long getTotalActiveProductCount() {
        return productRepository.countAllActiveProducts();
    }

    public Map<String, Long> getProductCountsByAllCategories() {
        List<Object[]> results = productRepository.getProductCountsByCategory();
        return results.stream().collect(Collectors.toMap(
                result -> (String) result[0],
                result -> ((Number) result[1]).longValue()
        ));
    }

    public List<ProductDetailDTO> getProductsForGuestCart(List<GuestCartItemDTO> cartItems) {
        List<String> productIds = cartItems.stream()
                .map(GuestCartItemDTO::getProductId)
                .distinct()
                .collect(Collectors.toList());

        if (productIds.isEmpty()) return new ArrayList<>();

        List<Product> products = productRepository.findByProductIdInAndProductStatus(productIds, "ACTIVE");
        return products.stream().map(this::convertToProductDetailDTO).collect(Collectors.toList());
    }

    // ===== 이미지 처리 메서드들 (변경 없음) =====

    public void attachImagesToProduct(ProductDto product) {
        try {
            ApiResponse<List<ProductImageDto>> imagesResponse = imageServiceClient.getProductImages(product.getProductId());
            if (imagesResponse.isSuccess() && imagesResponse.getData() != null) {
                product.setProductImages(imagesResponse.getData());
                product.setImages(imagesResponse.getData().stream()
                        .map(ProductImageDto::getImageUrl)
                        .collect(Collectors.toList()));
            } else {
                product.setProductImages(new ArrayList<>());
                product.setImages(new ArrayList<>());
            }

            ApiResponse<ProductImageDto> mainImageResponse = imageServiceClient.getMainImage(product.getProductId());
            if (mainImageResponse.isSuccess() && mainImageResponse.getData() != null) {
                ProductImageDto mainImageDto = mainImageResponse.getData();
                product.setMainImage(mainImageDto.getImageUrl());
                product.setImage(mainImageDto.getImageUrl());
            }
        } catch (Exception e) {
            log.warn("이미지 조회 실패: {}", product.getProductId(), e);
            product.setProductImages(new ArrayList<>());
            product.setImages(new ArrayList<>());
        }
    }

    public void attachMainImagesToProducts(List<ProductDto> products) {
        if (products == null || products.isEmpty()) return;

        List<String> productIds = products.stream()
                .map(ProductDto::getProductId)
                .collect(Collectors.toList());

        ApiResponse<Map<String, ProductImageDto>> mainImagesResponse =
                imageServiceClient.getMainImages(productIds);

        if (mainImagesResponse.isSuccess() && mainImagesResponse.getData() != null) {
            Map<String, ProductImageDto> mainImages = mainImagesResponse.getData();
            products.forEach(product -> {
                ProductImageDto mainImage = mainImages.get(product.getProductId());
                if (mainImage != null) {
                    String completeImageUrl = buildCompleteImageUrl(mainImage);
                    product.setMainImage(completeImageUrl);
                    product.setImage(completeImageUrl);
                    product.setImages(Collections.singletonList(completeImageUrl));
                } else {
                    setDefaultImage(product);
                }
            });
        } else {
            products.forEach(this::setDefaultImage);
        }
    }

    // ===== 유틸리티 메서드들 (변경 없음) =====

    private boolean isMainCategory(String categoryId) {
        if (categoryId == null || categoryId.trim().isEmpty()) {
            return false;
        }
        return categoryId.length() == 1 && categoryId.matches("[1-9]");
    }

    private ProductDto convertToDto(Product product) {
        Integer discount = calculateDiscountRate(product.getPrice(), product.getSalePrice());
        return ProductDto.builder()
                .productId(product.getProductId())
                .categoryId(product.getCategoryId())
                .name(product.getName())
                .title(product.getName())
                .price(product.getPrice())
                .salePrice(product.getSalePrice())
                .originalPrice(product.getPrice())
                .productDescription(product.getProductDescription())
                .productShortDescription(product.getProductShortDescription())
                .productStatus(product.getProductStatus())
                .productSalesCount(product.getProductSalesCount())
                .productRating(product.getProductRating())
                .productReviewCount(product.getProductReviewCount())
                .createdDate(product.getCreatedDate())
                .updatedDate(product.getUpdatedDate())
                .startDate(product.getStartDate())
                .endDate(product.getEndDate())
                .mainImage(product.getMainImage())
                .image(getProductImageUrl(product))
                .viewCount(product.getViewCount())
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
                .stockQuantity(999L)
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
                .build();
    }

    private List<ProductDto> convertToDtoList(List<Product> products) {
        return products == null ? new ArrayList<>() :
                products.stream()
                        .map(this::convertToDto)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
    }

    private List<ProductDto> convertToDtoListWithImages(List<Product> products) {
        List<ProductDto> productDtos = convertToDtoList(products);
        attachMainImagesToProducts(productDtos);
        return productDtos;
    }

    private String buildCompleteImageUrl(ProductImageDto imageDto) {
        if (imageDto == null) return getDefaultImageUrl();

        String imageUrl = imageDto.getImageUrl();
        String fileName = imageDto.getFileName();

        if (fileName != null && !fileName.trim().isEmpty()) {
            return "http://localhost:8080/api/images/products/" + fileName;
        }

        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            if (imageUrl.startsWith("http")) {
                return imageUrl;
            }
            if (imageUrl.startsWith("/")) {
                return "http://localhost:8080" + imageUrl;
            }
            return "http://localhost:8080/api/images/products/" + imageUrl;
        }

        return getDefaultImageUrl();
    }

    private void setDefaultImage(ProductDto product) {
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
            // [브랜드명] 패턴 찾기
            if (productName.contains("[") && productName.contains("]")) {
                int start = productName.indexOf("[") + 1;
                int end = productName.indexOf("]");
                if (start < end) {
                    return productName.substring(start, end);
                }
            }
            // (브랜드명) 패턴 찾기
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

    public void attachAllImagesToProducts(List<ProductDto> products) {
        if (products == null || products.isEmpty()) return;

        List<String> productIds = products.stream()
                .map(ProductDto::getProductId)
                .collect(Collectors.toList());

        ApiResponse<Map<String, List<ProductImageDto>>> allImagesResponse =
                imageServiceClient.getProductImages(productIds);
        ApiResponse<Map<String, ProductImageDto>> mainImagesResponse =
                imageServiceClient.getMainImages(productIds);

        if (allImagesResponse.isSuccess() && allImagesResponse.getData() != null) {
            Map<String, List<ProductImageDto>> allImages = allImagesResponse.getData();
            Map<String, ProductImageDto> mainImages =
                    mainImagesResponse.isSuccess() ? mainImagesResponse.getData() : new HashMap<>();

            products.forEach(product -> {
                String productId = product.getProductId();

                // 모든 이미지 설정
                List<ProductImageDto> productImageDtos = allImages.getOrDefault(productId, new ArrayList<>());
                product.setProductImages(productImageDtos);
                product.setImages(productImageDtos.stream()
                        .map(ProductImageDto::getImageUrl)
                        .collect(Collectors.toList()));

                // 메인 이미지 설정
                ProductImageDto mainImage = mainImages.get(productId);
                if (mainImage != null) {
                    product.setMainImage(mainImage.getImageUrl());
                    product.setImage(mainImage.getImageUrl());
                } else if (productImageDtos.isEmpty()) {
                    // 이미지가 없으면 기본 이미지 설정
                    setDefaultImage(product);
                }
            });
        } else {
            // 이미지 조회 실패 시 모든 상품에 기본 이미지 설정
            products.forEach(this::setDefaultImage);
        }
    }
}