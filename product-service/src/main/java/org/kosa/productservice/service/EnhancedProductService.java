package org.kosa.productservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kosa.productservice.dto.ProductDTO;
import org.springframework.stereotype.Service;

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
    public ProductDTO getProductDetail(Integer productId) {
        // 기존 상품 정보 조회
        ProductDTO product = productService.getProductById(productId);
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
    public List<ProductDTO> getProductList(int limit) {
        // 기존 상품 목록 조회
        List<ProductDTO> products = productService.getAllProducts(limit);

        // 대표 이미지 정보 추가
        imageIntegrationService.attachMainImagesToProducts(products);

        return products;
    }

    /**
     * 카테고리별 상품 조회 (대표 이미지 포함)
     */
    public List<ProductDTO> getProductsByCategory(Integer categoryId, int limit) {
        // 기존 카테고리별 상품 조회
        List<ProductDTO> products = productService.getProductsByCategory(categoryId, limit);

        // 대표 이미지 정보 추가
        imageIntegrationService.attachMainImagesToProducts(products);

        return products;
    }

    /**
     * 연관 상품 조회 (대표 이미지 포함)
     */
    public List<ProductDTO> getRelatedProducts(Integer productId, int limit) {
        // 기존 연관 상품 조회
        List<ProductDTO> products = productService.getRelatedProducts(productId, limit);

        // 대표 이미지 정보 추가
        imageIntegrationService.attachMainImagesToProducts(products);

        return products;
    }

    /**
     * HOST별 상품 조회 (대표 이미지 포함)
     */
    public List<ProductDTO> getProductsByHost(Long hostId, int limit) {
        // 기존 HOST별 상품 조회
        List<ProductDTO> products = productService.getProductsByHost(hostId, limit);

        // 대표 이미지 정보 추가
        imageIntegrationService.attachMainImagesToProducts(products);

        return products;
    }

    /**
     * 재고가 있는 상품 조회 (대표 이미지 포함)
     */
    public List<ProductDTO> getProductsInStock(int limit) {
        // 재고가 있는 상품 조회
        List<ProductDTO> products = productService.getProductsInStock(limit);

        // 대표 이미지 정보 추가
        imageIntegrationService.attachMainImagesToProducts(products);

        return products;
    }

    /**
     * 할인 상품 조회 (대표 이미지 포함)
     */
    public List<ProductDTO> getDiscountedProducts(int limit) {
        // 할인 상품 조회
        List<ProductDTO> products = productService.getDiscountedProducts(limit);

        // 대표 이미지 정보 추가
        imageIntegrationService.attachMainImagesToProducts(products);

        return products;
    }

    /**
     * 인기 상품 조회 (대표 이미지 포함)
     */
    public List<ProductDTO> getPopularProducts(int limit) {
        // 인기 상품 조회
        List<ProductDTO> products = productService.getPopularProducts(limit);

        // 대표 이미지 정보 추가
        imageIntegrationService.attachMainImagesToProducts(products);

        return products;
    }

    /**
     * 여러 상품에 메인 이미지 첨부 (외부 호출용)
     */
    public void attachMainImagesToProducts(List<ProductDTO> products) {
        imageIntegrationService.attachMainImagesToProducts(products);
    }

    /**
     * 단일 상품에 모든 이미지 첨부 (외부 호출용)
     */
    public void attachAllImagesToProduct(ProductDTO product) {
        imageIntegrationService.attachImagesToProduct(product);
    }

    /**
     * 여러 상품에 모든 이미지 첨부 (외부 호출용)
     */
    public void attachAllImagesToProducts(List<ProductDTO> products) {
        imageIntegrationService.attachAllImagesToProducts(products);
    }

    /**
     * 이미지 서비스 연결 상태 확인
     */
    public boolean isImageServiceAvailable() {
        return imageIntegrationService.isImageServiceAvailable();
    }
}