package org.kosa.productservice.service;

import lombok.RequiredArgsConstructor;
import org.kosa.productservice.dto.Product;
import org.kosa.productservice.dto.ProductDto;
import org.kosa.productservice.mapper.ProductRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;


@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
//    private final CategoryService categoryService; // Feign Client로 호출

    // 전체 상품 조회 (라이브/일반 필터링)
    public List<ProductDto> getAllProducts(String filter, int limit) {
        LocalDateTime now = LocalDateTime.now();
        Pageable pageable = PageRequest.of(0, limit);

        List<Product> products;
        if ("live".equals(filter)) {
            products = productRepository.findLiveProducts("ACTIVE", now, pageable);
        } else {
            products = productRepository.findByProductStatusOrderByCreatedDateDesc("ACTIVE")
                    .stream().limit(limit).collect(Collectors.toList());
        }

        return products.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    // 카테고리별 상품 조회
    public List<ProductDto> getProductsByCategory(String categoryId, String subcategoryId, String filter, int limit) {
        LocalDateTime now = LocalDateTime.now();
        Pageable pageable = PageRequest.of(0, limit);

        List<String> targetCategoryIds = new ArrayList<>();

        if (subcategoryId != null && !"ALL".equals(subcategoryId)) {
            targetCategoryIds.add(subcategoryId);
        } else if (categoryId != null && !"ALL".equals(categoryId)) {
            targetCategoryIds.add(categoryId);
            // 하위 카테고리도 포함
            List<CategoryDto> subCategories = categoryService.getSubCategories(categoryId);
            targetCategoryIds.addAll(subCategories.stream()
                    .map(CategoryDto::getCategoryId)
                    .collect(Collectors.toList()));
        }

        List<Product> products;
        if ("live".equals(filter)) {
            if (targetCategoryIds.isEmpty()) {
                products = productRepository.findLiveProducts("ACTIVE", now, pageable);
            } else {
                products = new ArrayList<>();
                for (String catId : targetCategoryIds) {
                    products.addAll(productRepository.findLiveProductsByCategory(catId, "ACTIVE", now, pageable));
                }
                products = products.stream()
                        .sorted((p1, p2) -> p2.getProductSalesCount().compareTo(p1.getProductSalesCount()))
                        .limit(limit)
                        .collect(Collectors.toList());
            }
        } else {
            if (targetCategoryIds.isEmpty()) {
                products = productRepository.findByProductStatusOrderByCreatedDateDesc("ACTIVE")
                        .stream().limit(limit).collect(Collectors.toList());
            } else {
                products = productRepository.findByCategoryIdsAndProductStatus(targetCategoryIds, "ACTIVE")
                        .stream().limit(limit).collect(Collectors.toList());
            }
        }

        return products.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    // 인기 상품 조회
    public List<ProductDto> getPopularProducts(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<Product> products = productRepository.findByProductStatusOrderByProductSalesCountDesc("ACTIVE", pageable);

        return products.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    // 상품 검색
    public List<ProductDto> searchProducts(String keyword) {
        List<Product> products = productRepository.searchProducts(keyword, "ACTIVE");

        return products.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    // 상품 상세 조회
    public ProductDto getProduct(String productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("상품을 찾을 수 없습니다: " + productId));

        return convertToDto(product);
    }

    private ProductDto convertToDto(Product product) {
        Integer discount = calculateDiscountRate(product.getPrice(), product.getSalePrice());
        Boolean isLive = isLiveProduct(product);

        return ProductDto.builder()
                .productId(product.getProductId())
                .categoryId(product.getCategoryId())
                .name(product.getName())
                .title(product.getName()) // 프론트엔드 호환성
                .price(product.getSalePrice() != null ? product.getSalePrice() : product.getPrice())
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
                .image(product.getMainImage()) // 프론트엔드 호환성
                .viewCount(product.getViewCount())
                .discount(discount)
                .isLive(isLive)
                .viewers(isLive ? generateViewersCount() : null)
                .build();
    }

    private Integer calculateDiscountRate(Integer originalPrice, Integer salePrice) {
        if (salePrice == null || originalPrice == null || originalPrice == 0) {
            return null;
        }
        return (int) Math.round(((double) (originalPrice - salePrice) / originalPrice) * 100);
    }

    private Boolean isLiveProduct(Product product) {
        if (product.getStartDate() == null) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        return product.getStartDate().isBefore(now) || product.getStartDate().isEqual(now) &&
                (product.getEndDate() == null || product.getEndDate().isAfter(now) || product.getEndDate().isEqual(now));
    }

    private String generateViewersCount() {
        // 실제로는 라이브 시청자 수를 별도 서비스에서 가져와야 함
        Random random = new Random();
        int viewers = random.nextInt(200) + 1; // 1~200 사이
        return viewers + "만";
    }
}
