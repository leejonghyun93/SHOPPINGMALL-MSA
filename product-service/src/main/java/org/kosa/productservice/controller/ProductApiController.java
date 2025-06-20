package org.kosa.productservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kosa.productservice.dto.GuestCartItemDTO;
import org.kosa.productservice.dto.ProductDetailDTO;
import org.kosa.productservice.dto.ProductDto;
import org.kosa.productservice.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Slf4j
public class ProductApiController {

    private final ProductService productService;

    // ================== 메인 API 엔드포인트들 ==================

    /**
     * 상품 상세 조회 (ID로 조회)
     */
    @GetMapping("/{productId}")
    public ResponseEntity<ProductDto> getProductDetail(@PathVariable String productId) {
        try {
            log.info("상품 상세 조회 요청 - productId: {}", productId);

            ProductDto product = productService.getProductById(productId);
            if (product == null) {
                log.warn("상품을 찾을 수 없음 - productId: {}", productId);
                return ResponseEntity.notFound().build();
            }

            // 조회수 증가
            productService.increaseViewCount(productId);

            log.info("상품 상세 조회 성공 - productId: {}", productId);
            return ResponseEntity.ok(product);
        } catch (Exception e) {
            log.error("상품 상세 조회 중 오류 - productId: {}", productId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 전체 상품 조회
     */
    @GetMapping
    public ResponseEntity<List<ProductDto>> getAllProducts(
            @RequestParam(defaultValue = "20") int limit) {

        try {
            log.info("전체 상품 조회 요청 - limit: {}", limit);
            List<ProductDto> products = productService.getAllProducts(limit);
            log.info("전체 상품 조회 결과: {}개", products.size());
            return ResponseEntity.ok(products);
        } catch (Exception e) {
            log.error("전체 상품 조회 중 오류:", e);
            return ResponseEntity.ok(List.of());
        }
    }

    /**
     * Vue 프론트엔드용 - 카테고리별 상품 조회
     */
    @GetMapping("/filter")
    public ResponseEntity<List<ProductDto>> getProductsByFilter(
            @RequestParam(defaultValue = "ALL") String categoryId,
            @RequestParam(defaultValue = "10") Integer limit) {

        try {
            log.info("카테고리별 상품 조회 - categoryId: {}, limit: {}", categoryId, limit);

            List<ProductDto> products;
            if ("ALL".equals(categoryId)) {
                products = productService.getAllProducts(limit);
            } else {
                products = productService.getProductsByCategory(categoryId, limit);
            }

            log.info("카테고리별 상품 조회 결과: {}개", products.size());
            return ResponseEntity.ok(products);
        } catch (Exception e) {
            log.error("카테고리별 상품 조회 중 오류:", e);
            return ResponseEntity.ok(List.of());
        }
    }

    /**
     * 카테고리별 상품 조회 (RESTful 스타일)
     */
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<ProductDto>> getProductsByCategory(
            @PathVariable String categoryId,
            @RequestParam(defaultValue = "20") int limit) {

        try {
            log.info("카테고리별 상품 조회 - categoryId: {}, limit: {}", categoryId, limit);
            List<ProductDto> products = productService.getProductsByCategory(categoryId, limit);
            log.info("카테고리별 상품 조회 결과: {}개", products.size());
            return ResponseEntity.ok(products);
        } catch (Exception e) {
            log.error("카테고리별 상품 조회 중 오류:", e);
            return ResponseEntity.ok(List.of());
        }
    }

    /**
     * 연관 상품 조회 (같은 카테고리의 다른 상품들)
     */
    @GetMapping("/{productId}/related")
    public ResponseEntity<List<ProductDto>> getRelatedProducts(
            @PathVariable String productId,
            @RequestParam(defaultValue = "4") int limit) {
        try {
            log.info("연관 상품 조회 요청 - productId: {}, limit: {}", productId, limit);

            List<ProductDto> relatedProducts = productService.getRelatedProducts(productId, limit);
            log.info("연관 상품 조회 결과: {}개", relatedProducts.size());
            return ResponseEntity.ok(relatedProducts);
        } catch (Exception e) {
            log.error("연관 상품 조회 중 오류 - productId: {}", productId, e);
            return ResponseEntity.ok(List.of());
        }
    }

    // ================== 통계 및 정보 API ==================

    /**
     * 카테고리별 상품 개수 조회
     */
    @GetMapping("/stats/count")
    public ResponseEntity<Map<String, Object>> getProductCount(
            @RequestParam(required = false) String categoryId) {

        Map<String, Object> result = new HashMap<>();
        try {
            if (categoryId != null && !"ALL".equals(categoryId)) {
                Long count = productService.getProductCountByCategory(categoryId);
                result.put("categoryId", categoryId);
                result.put("productCount", count);
                log.info("카테고리 {} 상품 개수: {}", categoryId, count);
            } else {
                Long totalCount = productService.getTotalActiveProductCount();
                result.put("totalActiveProducts", totalCount);
                log.info("전체 활성 상품 개수: {}", totalCount);
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("상품 개수 조회 중 오류:", e);
            result.put("error", e.getMessage());
            return ResponseEntity.ok(result);
        }
    }

    /**
     * 전체 카테고리별 상품 개수 통계
     */
    @GetMapping("/stats/count-all")
    public ResponseEntity<Map<String, Long>> getAllCategoryProductCounts() {
        try {
            log.info("전체 카테고리별 상품 개수 통계 요청");
            Map<String, Long> counts = productService.getProductCountsByAllCategories();
            log.info("카테고리별 상품 개수 통계 결과: {} 카테고리", counts.size());
            return ResponseEntity.ok(counts);
        } catch (Exception e) {
            log.error("카테고리별 상품 개수 통계 조회 중 오류:", e);
            return ResponseEntity.ok(new HashMap<>());
        }
    }

    /**
     * 디버그용: 데이터베이스 상태 확인
     */
    @GetMapping("/debug/status")
    public ResponseEntity<Map<String, Object>> getDatabaseStatus() {
        Map<String, Object> result = new HashMap<>();
        try {
            // 전체 상품 개수
            Long totalProducts = productService.getTotalActiveProductCount();
            result.put("totalActiveProducts", totalProducts);

            // 카테고리별 상품 개수
            Map<String, Long> categoryStats = productService.getProductCountsByAllCategories();
            result.put("categoryStats", categoryStats);

            log.info("데이터베이스 상태: 전체 {}개, 카테고리별 {}", totalProducts, categoryStats);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("데이터베이스 상태 확인 중 오류:", e);
            result.put("error", e.getMessage());
            return ResponseEntity.ok(result);
        }
    }
    // ================== 신규 추가: 게스트 장바구니 상세 조회 ==================

    // ProductApiController에 임시 추가
    @PostMapping("/test-simple")
    public ResponseEntity<String> testSimple(@RequestBody String rawData) {
        log.info("🔍 받은 데이터: {}", rawData);
        return ResponseEntity.ok("OK");
    }

    // 그리고 기존 guest-cart-details를 잠시 이렇게 수정
    @PostMapping("/guest-cart-details")
    public ResponseEntity<List<ProductDetailDTO>> getGuestCartDetails(@RequestBody List<GuestCartItemDTO> cartItems) {
        log.info("🔍 게스트 장바구니 상세 조회 요청: {}", cartItems);

        try {
            List<ProductDetailDTO> result = productService.getProductsForGuestCart(cartItems);
            log.info("🔍 게스트 장바구니 상세 조회 결과: {}개 상품", result.size());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("🔥 게스트 장바구니 처리 실패: ", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}