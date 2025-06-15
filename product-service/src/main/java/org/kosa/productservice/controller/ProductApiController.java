package org.kosa.productservice.controller;

import lombok.RequiredArgsConstructor;

import org.kosa.productservice.dto.ProductDto;
import org.kosa.productservice.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ProductApiController {

    private final ProductService productService;

    // 전체 상품 조회
    @GetMapping
    public ResponseEntity<List<ProductDto>> getAllProducts(
            @RequestParam(defaultValue = "live") String filter,
            @RequestParam(defaultValue = "20") int limit) {

        List<ProductDto> products = productService.getAllProducts(filter, limit);
        return ResponseEntity.ok(products);
    }

    // 카테고리별 상품 조회
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<ProductDto>> getProductsByCategory(
            @PathVariable String categoryId,
            @RequestParam(required = false) String subcategoryId,
            @RequestParam(defaultValue = "live") String filter,
            @RequestParam(defaultValue = "20") int limit) {

        List<ProductDto> products = productService.getProductsByCategory(categoryId, subcategoryId, filter, limit);
        return ResponseEntity.ok(products);
    }

    // 인기 상품 조회
    @GetMapping("/popular")
    public ResponseEntity<List<ProductDto>> getPopularProducts(@RequestParam(defaultValue = "20") int limit) {
        List<ProductDto> products = productService.getPopularProducts(limit);
        return ResponseEntity.ok(products);
    }

    // 상품 검색
    @GetMapping("/search")
    public ResponseEntity<List<ProductDto>> searchProducts(@RequestParam String keyword) {
        List<ProductDto> products = productService.searchProducts(keyword);
        return ResponseEntity.ok(products);
    }

    // 상품 상세 조회
    @GetMapping("/{productId}")
    public ResponseEntity<ProductDto> getProduct(@PathVariable String productId) {
        ProductDto product = productService.getProduct(productId);
        return ResponseEntity.ok(product);
    }
}