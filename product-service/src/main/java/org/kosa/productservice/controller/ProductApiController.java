package org.kosa.productservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kosa.productservice.dto.ApiResponse;
import org.kosa.productservice.dto.GuestCartItemDTO;
import org.kosa.productservice.dto.ProductDetailDTO;
import org.kosa.productservice.dto.ProductDTO;
import org.kosa.productservice.service.EnhancedProductService;
import org.kosa.productservice.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Slf4j
public class ProductApiController {

    private final ProductService productService;
    private final EnhancedProductService enhancedProductService;

    // ================== 메인 API 엔드포인트들 ==================

    /**
     * 상품 상세 조회 (기본 - 하위 호환성 유지)
     */
    @GetMapping("/{productId}")
    public ResponseEntity<ProductDTO> getProductDetail(@PathVariable Integer productId) {
        try {
            log.info("상품 상세 조회 요청 - productId: {}", productId);

            ProductDTO product = productService.getProductById(productId);
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
     * 상품 상세 조회 (이미지 포함)
     */
    @GetMapping("/{productId}/with-images")
    public ResponseEntity<ApiResponse<ProductDTO>> getProductDetailWithImages(@PathVariable Integer productId) {
        try {
            log.info("상품 상세 조회 (이미지 포함) 요청 - productId: {}", productId);

            ProductDTO product = enhancedProductService.getProductDetail(productId);
            if (product == null) {
                log.warn("상품을 찾을 수 없음 - productId: {}", productId);
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("상품을 찾을 수 없습니다."));
            }

            log.info("상품 상세 조회 (이미지 포함) 성공 - productId: {}", productId);
            return ResponseEntity.ok(ApiResponse.success(product));
        } catch (Exception e) {
            log.error("상품 상세 조회 (이미지 포함) 실패 - productId: {}, error: {}", productId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("상품 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * 전체 상품 조회
     */
    @GetMapping
    public ResponseEntity<List<ProductDTO>> getAllProducts(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "false") boolean includeImages) {

        try {
            log.info("전체 상품 조회 요청 - limit: {}, includeImages: {}", limit, includeImages);

            List<ProductDTO> products;
            if (includeImages) {
                products = enhancedProductService.getProductList(limit);
            } else {
                products = productService.getAllProducts(limit);
            }

            log.info("전체 상품 조회 결과: {}개", products.size());
            return ResponseEntity.ok(products);
        } catch (Exception e) {
            log.error("전체 상품 조회 중 오류:", e);
            return ResponseEntity.ok(List.of());
        }
    }

    /**
     * 전체 상품 조회 (이미지 포함 - API Response 형태)
     */
    @GetMapping("/with-images")
    public ResponseEntity<ApiResponse<List<ProductDTO>>> getAllProductsWithImages(
            @RequestParam(defaultValue = "20") int limit) {

        try {
            log.info("전체 상품 조회 (이미지 포함) 요청 - limit: {}", limit);

            List<ProductDTO> products = enhancedProductService.getProductList(limit);

            log.info("전체 상품 조회 (이미지 포함) 결과: {}개", products.size());
            return ResponseEntity.ok(ApiResponse.success(products));
        } catch (Exception e) {
            log.error("전체 상품 조회 (이미지 포함) 실패 - error: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("상품 목록 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * Vue 프론트엔드용 - 카테고리별 상품 조회
     */
    @GetMapping("/filter")
    public ResponseEntity<List<ProductDTO>> getProductsByFilter(
            @RequestParam(defaultValue = "ALL") String categoryIdStr,
            @RequestParam(defaultValue = "10") Integer limit,
            @RequestParam(defaultValue = "false") boolean includeImages) {

        try {
            log.info("카테고리별 상품 조회 - categoryId: {}, limit: {}, includeImages: {}", categoryIdStr, limit, includeImages);

            List<ProductDTO> products;
            if (includeImages) {
                if ("ALL".equals(categoryIdStr)) {
                    products = enhancedProductService.getProductList(limit);
                } else {
                    Integer categoryId = Integer.parseInt(categoryIdStr);
                    products = enhancedProductService.getProductsByCategory(categoryId, limit);
                }
            } else {
                if ("ALL".equals(categoryIdStr)) {
                    products = productService.getAllProducts(limit);
                } else {
                    Integer categoryId = Integer.parseInt(categoryIdStr);
                    products = productService.getProductsByCategory(categoryId, limit);
                }
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
    public ResponseEntity<List<ProductDTO>> getProductsByCategory(
            @PathVariable Integer categoryId,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "false") boolean includeImages) {

        try {
            log.info("카테고리별 상품 조회 - categoryId: {}, limit: {}, includeImages: {}", categoryId, limit, includeImages);

            List<ProductDTO> products;
            if (includeImages) {
                products = enhancedProductService.getProductsByCategory(categoryId, limit);
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
     * 카테고리별 상품 조회 (이미지 포함 - API Response 형태)
     */
    @GetMapping("/category/{categoryId}/with-images")
    public ResponseEntity<ApiResponse<List<ProductDTO>>> getProductsByCategoryWithImages(
            @PathVariable Integer categoryId,
            @RequestParam(defaultValue = "20") int limit) {

        try {
            log.info("카테고리별 상품 조회 (이미지 포함) - categoryId: {}, limit: {}", categoryId, limit);

            List<ProductDTO> products = enhancedProductService.getProductsByCategory(categoryId, limit);

            log.info("카테고리별 상품 조회 (이미지 포함) 결과: {}개", products.size());
            return ResponseEntity.ok(ApiResponse.success(products));
        } catch (Exception e) {
            log.error("카테고리별 상품 조회 (이미지 포함) 실패 - categoryId: {}, error: {}", categoryId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("카테고리별 상품 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * 연관 상품 조회 (같은 카테고리의 다른 상품들)
     */
    @GetMapping("/{productId}/related")
    public ResponseEntity<List<ProductDTO>> getRelatedProducts(
            @PathVariable Integer productId,
            @RequestParam(defaultValue = "4") int limit,
            @RequestParam(defaultValue = "false") boolean includeImages) {
        try {
            log.info("연관 상품 조회 요청 - productId: {}, limit: {}, includeImages: {}", productId, limit, includeImages);

            List<ProductDTO> relatedProducts;
            if (includeImages) {
                relatedProducts = enhancedProductService.getRelatedProducts(productId, limit);
            } else {
                relatedProducts = productService.getRelatedProducts(productId, limit);
            }

            log.info("연관 상품 조회 결과: {}개", relatedProducts.size());
            return ResponseEntity.ok(relatedProducts);
        } catch (Exception e) {
            log.error("연관 상품 조회 중 오류 - productId: {}", productId, e);
            return ResponseEntity.ok(List.of());
        }
    }

    /**
     * 연관 상품 조회 (이미지 포함 - API Response 형태)
     */
    @GetMapping("/{productId}/related/with-images")
    public ResponseEntity<ApiResponse<List<ProductDTO>>> getRelatedProductsWithImages(
            @PathVariable Integer productId,
            @RequestParam(defaultValue = "4") int limit) {
        try {
            log.info("연관 상품 조회 (이미지 포함) 요청 - productId: {}, limit: {}", productId, limit);

            List<ProductDTO> relatedProducts = enhancedProductService.getRelatedProducts(productId, limit);

            log.info("연관 상품 조회 (이미지 포함) 결과: {}개", relatedProducts.size());
            return ResponseEntity.ok(ApiResponse.success(relatedProducts));
        } catch (Exception e) {
            log.error("연관 상품 조회 (이미지 포함) 실패 - productId: {}, error: {}", productId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("연관 상품 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * 추천 상품 조회 (이미지 포함)
     */
    @GetMapping("/recommended")
    public ResponseEntity<ApiResponse<List<ProductDTO>>> getRecommendedProducts(
            @RequestParam(required = false) String userId,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            log.info("추천 상품 조회 요청 - userId: {}, limit: {}", userId, limit);

            // 현재는 최신 상품으로 대체 (추천 로직은 별도 구현 필요)
            List<ProductDTO> products = enhancedProductService.getProductList(limit);

            log.info("추천 상품 조회 결과: {}개", products.size());
            return ResponseEntity.ok(ApiResponse.success(products));
        } catch (Exception e) {
            log.error("추천 상품 조회 실패 - userId: {}, error: {}", userId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("추천 상품 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * HOST별 상품 조회
     */
    @GetMapping("/host/{hostId}")
    public ResponseEntity<List<ProductDTO>> getProductsByHost(
            @PathVariable Long hostId,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "false") boolean includeImages) {
        try {
            log.info("HOST별 상품 조회 요청 - hostId: {}, limit: {}, includeImages: {}", hostId, limit, includeImages);

            List<ProductDTO> products = productService.getProductsByHost(hostId, limit);

            if (includeImages) {
                enhancedProductService.attachMainImagesToProducts(products);
            }

            log.info("HOST별 상품 조회 결과: {}개", products.size());
            return ResponseEntity.ok(products);
        } catch (Exception e) {
            log.error("HOST별 상품 조회 중 오류 - hostId: {}", hostId, e);
            return ResponseEntity.ok(List.of());
        }
    }

    // ================== 통계 및 정보 API ==================

    /**
     * 카테고리별 상품 개수 조회
     */
    @GetMapping("/stats/count")
    public ResponseEntity<Map<String, Object>> getProductCount(
            @RequestParam(required = false) String categoryIdStr) {

        Map<String, Object> result = new HashMap<>();
        try {
            if (categoryIdStr != null && !"ALL".equals(categoryIdStr)) {
                Integer categoryId = Integer.parseInt(categoryIdStr);
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
    public ResponseEntity<Map<Integer, Long>> getAllCategoryProductCounts() {
        try {
            log.info("전체 카테고리별 상품 개수 통계 요청");
            Map<Integer, Long> counts = productService.getProductCountsByAllCategories();
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
            Map<Integer, Long> categoryStats = productService.getProductCountsByAllCategories();
            result.put("categoryStats", categoryStats);

            log.info("데이터베이스 상태: 전체 {}개, 카테고리별 {}", totalProducts, categoryStats);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("데이터베이스 상태 확인 중 오류:", e);
            result.put("error", e.getMessage());
            return ResponseEntity.ok(result);
        }
    }

    // ================== 게스트 장바구니 관련 API ==================

    /**
     * 게스트 장바구니 상세 조회
     */
    @PostMapping("/guest-cart-details")
    public ResponseEntity<List<ProductDetailDTO>> getGuestCartDetails(@RequestBody List<GuestCartItemDTO> cartItems) {
        log.info("게스트 장바구니 상세 조회 요청: {}개 상품", cartItems.size());

        try {
            List<ProductDetailDTO> result = productService.getProductsForGuestCart(cartItems);
            log.info("게스트 장바구니 상세 조회 결과: {}개 상품", result.size());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("게스트 장바구니 처리 실패: ", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 테스트용 API
     */
    @PostMapping("/test-simple")
    public ResponseEntity<String> testSimple(@RequestBody String rawData) {
        log.info("받은 데이터: {}", rawData);
        return ResponseEntity.ok("OK");
    }
}