package org.kosa.productservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kosa.productservice.client.CategoryServiceClient;
import org.kosa.productservice.dto.GuestCartItemDTO;
import org.kosa.productservice.dto.Product;
import org.kosa.productservice.dto.ProductDetailDTO;
import org.kosa.productservice.dto.ProductDto;
import org.kosa.productservice.mapper.ProductRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryServiceClient categoryServiceClient;

    // ================== 메인 비즈니스 메서드들 ==================

    /**
     * 상품 ID로 상품 상세 조회
     */
    public ProductDto getProductById(String productId) {
        try {
            log.info("상품 상세 조회 - productId: {}", productId);

            Optional<Product> productOpt = productRepository.findByProductIdAndProductStatus(productId, "ACTIVE");
            if (productOpt.isPresent()) {
                ProductDto dto = convertToDto(productOpt.get());
                log.info("상품 상세 조회 성공 - productId: {}", productId);
                return dto;
            }

            log.warn("상품을 찾을 수 없음 - productId: {}", productId);
            return null;
        } catch (Exception e) {
            log.error("상품 상세 조회 실패 - productId: {}", productId, e);
            throw new RuntimeException("상품 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 연관 상품 조회 (같은 카테고리의 다른 상품들)
     */
    public List<ProductDto> getRelatedProducts(String productId, int limit) {
        try {
            log.info("연관 상품 조회 - productId: {}, limit: {}", productId, limit);

            // 1. 먼저 해당 상품의 카테고리 조회
            Optional<Product> currentProductOpt = productRepository.findByProductIdAndProductStatus(productId, "ACTIVE");
            if (!currentProductOpt.isPresent()) {
                log.warn("기준 상품을 찾을 수 없음 - productId: {}", productId);
                return new ArrayList<>();
            }

            Product currentProduct = currentProductOpt.get();
            String categoryId = currentProduct.getCategoryId();

            // 2. 같은 카테고리의 다른 상품들 조회 (현재 상품 제외)
            Pageable pageable = PageRequest.of(0, limit);
            List<Product> relatedProducts = productRepository.findByCategoryIdAndProductStatusAndProductIdNot(
                    categoryId, "ACTIVE", productId, pageable
            );

            List<ProductDto> result = relatedProducts.stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());

            log.info("연관 상품 조회 완료 - 결과: {}개", result.size());
            return result;
        } catch (Exception e) {
            log.error("연관 상품 조회 실패 - productId: {}", productId, e);
            return new ArrayList<>();
        }
    }

    /**
     * 상품 조회수 증가
     */
    @Transactional
    public void increaseViewCount(String productId) {
        try {
            Optional<Product> productOpt = productRepository.findByProductIdAndProductStatus(productId, "ACTIVE");
            if (productOpt.isPresent()) {
                Product product = productOpt.get();
                // 조회수 필드가 있다면 증가
                // product.setViewCount(product.getViewCount() + 1);
                // productRepository.save(product);
            }
        } catch (Exception e) {
            log.error("상품 조회수 증가 실패 - productId: {}", productId, e);
        }
    }

    /**
     * 전체 상품 조회
     */
    public List<ProductDto> getAllProducts(int limit) {
        try {
            Pageable pageable = PageRequest.of(0, limit);
            List<Product> products = productRepository.findAllActiveProducts(pageable);
            return convertToDtoList(products);
        } catch (Exception e) {
            log.error("전체 상품 조회 실패:", e);
            return new ArrayList<>();
        }
    }

    /**
     * 카테고리별 상품 조회
     */
    public List<ProductDto> getProductsByCategory(String categoryId, int limit) {
        try {
            log.info("카테고리별 상품 조회 - categoryId: {}, limit: {}", categoryId, limit);

            // ALL 카테고리인 경우 전체 상품 조회
            if ("ALL".equals(categoryId)) {
                return getAllProducts(limit);
            }

            Pageable pageable = PageRequest.of(0, limit);
            List<Product> products = new ArrayList<>();

            // 카테고리 ID 길이로 레벨 판단
            if (categoryId.length() == 1) {
                // 메인 카테고리 - 하위 카테고리 포함 조회
                List<String> childrenCategoryIds = categoryServiceClient.getChildrenCategoryIds(categoryId);

                if (!childrenCategoryIds.isEmpty()) {
                    products = productRepository.findByMultipleCategoriesActive(childrenCategoryIds, pageable);
                } else {
                    products = productRepository.findByCategoryIdActive(categoryId, pageable);
                }
            } else {
                // 하위 카테고리 - 정확한 카테고리 ID로 조회
                products = productRepository.findByCategoryIdActive(categoryId, pageable);
            }

            return convertToDtoList(products);
        } catch (Exception e) {
            log.error("카테고리별 상품 조회 실패:", e);
            return new ArrayList<>();
        }
    }

    /**
     * 카테고리별 상품 개수 조회
     */
    public Long getProductCountByCategory(String categoryId) {
        try {
            if ("ALL".equals(categoryId)) {
                return productRepository.countAllActiveProducts();
            }
            return productRepository.countByCategoryActive(categoryId);
        } catch (Exception e) {
            log.error("카테고리별 상품 개수 조회 실패:", e);
            return 0L;
        }
    }

    /**
     * 전체 활성 상품 개수 조회
     */
    public Long getTotalActiveProductCount() {
        try {
            return productRepository.countAllActiveProducts();
        } catch (Exception e) {
            log.error("전체 활성 상품 개수 조회 실패:", e);
            return 0L;
        }
    }

    /**
     * 카테고리별 상품 개수 통계
     */
    public Map<String, Long> getProductCountsByAllCategories() {
        try {
            List<Object[]> results = productRepository.getProductCountsByCategory();
            return results.stream()
                    .collect(Collectors.toMap(
                            result -> (String) result[0],
                            result -> ((Number) result[1]).longValue()
                    ));
        } catch (Exception e) {
            log.error("카테고리별 상품 개수 조회 실패:", e);
            return new HashMap<>();
        }
    }

    // ================== 변환 메서드들 ==================

    private List<ProductDto> convertToDtoList(List<Product> products) {
        if (products == null || products.isEmpty()) {
            return new ArrayList<>();
        }

        return products.stream()
                .map(this::convertToDto)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private ProductDto convertToDto(Product product) {
        try {
            Integer discount = calculateDiscountRate(product.getPrice(), product.getSalePrice());

            return ProductDto.builder()
                    // 기존 필드들
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

                    // 상품 상세보기용 추가 필드들
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
        } catch (Exception e) {
            log.error("상품 DTO 변환 실패 - productId: {}", product.getProductId(), e);
            return null;
        }
    }

    private String getProductImageUrl(Product product) {
        if (product.getMainImage() != null && !product.getMainImage().trim().isEmpty()) {
            return product.getMainImage();
        }
        return "https://via.placeholder.com/300x200?text=" +
                (product.getName() != null ? product.getName().substring(0, Math.min(product.getName().length(), 10)) : "No+Image");
    }

    private Integer calculateDiscountRate(Integer originalPrice, Integer salePrice) {
        if (salePrice == null || originalPrice == null || originalPrice == 0) {
            return null;
        }
        return (int) Math.round(((double) (originalPrice - salePrice) / originalPrice) * 100);
    }

    /**
     * 상품명에서 브랜드 추출
     */
    private String extractBrandFromName(String productName) {
        if (productName != null) {
            // [브랜드명] 형태
            if (productName.contains("[") && productName.contains("]")) {
                int start = productName.indexOf("[") + 1;
                int end = productName.indexOf("]");
                if (start < end && start > 0) {
                    return productName.substring(start, end);
                }
            }

            // (브랜드명) 형태
            if (productName.contains("(") && productName.contains(")")) {
                int start = productName.indexOf("(") + 1;
                int end = productName.indexOf(")");
                if (start < end && start > 0) {
                    return productName.substring(start, end);
                }
            }
        }
        return "브랜드명";
    }

    /**
     * 메인 이미지를 기반으로 이미지 리스트 생성
     */
    private List<String> createImageList(String mainImage) {
        List<String> images = new ArrayList<>();
        if (mainImage != null && !mainImage.trim().isEmpty()) {
            images.add(mainImage);
        } else {
            images.add("https://via.placeholder.com/600x600?text=상품+이미지");
        }
        return images;
    }

    public List<ProductDetailDTO> getProductsForGuestCart(List<GuestCartItemDTO> cartItems) {
        // 상품 ID 목록 추출
        List<String> productIds = cartItems.stream()
                .map(GuestCartItemDTO::getProductId)
                .distinct()
                .collect(Collectors.toList());

        if (productIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 해당 상품들 ACTIVE 상태로 조회
        List<Product> products = productRepository.findByProductIdInAndProductStatus(productIds, "ACTIVE");

        // Product -> ProductDetailDTO 변환 (필요에 따라 변환 코드 작성)
        List<ProductDetailDTO> productDetails = products.stream()
                .map(product -> convertToProductDetailDTO(product))
                .collect(Collectors.toList());

        return productDetails;
    }

    // 변환 메서드 예시
    private ProductDetailDTO convertToProductDetailDTO(Product product) {
        return ProductDetailDTO.builder()
                .productId(product.getProductId())
                .name(product.getName())
                .price(product.getPrice())
                .salePrice(product.getSalePrice())
                .mainImage(product.getMainImage())
                .description(product.getProductDescription())
                // 필요한 필드들 추가
                .build();
    }
}