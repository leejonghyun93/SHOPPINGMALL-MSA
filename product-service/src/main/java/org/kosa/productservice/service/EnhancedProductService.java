package org.kosa.productservice.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kosa.productservice.dto.ProductDto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EnhancedProductService {

    private final ProductService productService; // 기존 상품 서비스
    private final ProductImageIntegrationService imageIntegrationService;

    /**
     * 상품 상세 조회 (이미지 포함)
     */
    public ProductDto getProductDetail(String productId) {
        // 기존 상품 정보 조회
        ProductDto product = productService.getProductById(productId);
        if (product == null) {
            return null;
        }

        // 이미지 정보 추가
        imageIntegrationService.attachImagesToProduct(product);

        return product;
    }

    /**
     * 상품 목록 조회 (대표 이미지 포함)
     */
    public List<ProductDto> getProductList(int limit) {
        // 기존 상품 목록 조회
        List<ProductDto> products = productService.getAllProducts(limit);

        // 대표 이미지 정보 추가
        imageIntegrationService.attachMainImagesToProducts(products);

        return products;
    }

    /**
     * 카테고리별 상품 조회 (대표 이미지 포함)
     */
    public List<ProductDto> getProductsByCategory(String categoryId, int limit) {
        // 기존 카테고리별 상품 조회
        List<ProductDto> products = productService.getProductsByCategory(categoryId, limit);

        // 대표 이미지 정보 추가
        imageIntegrationService.attachMainImagesToProducts(products);

        return products;
    }

    /**
     * 연관 상품 조회 (대표 이미지 포함)
     */
    public List<ProductDto> getRelatedProducts(String productId, int limit) {
        // 기존 연관 상품 조회
        List<ProductDto> products = productService.getRelatedProducts(productId, limit);

        // 대표 이미지 정보 추가
        imageIntegrationService.attachMainImagesToProducts(products);

        return products;
    }
}