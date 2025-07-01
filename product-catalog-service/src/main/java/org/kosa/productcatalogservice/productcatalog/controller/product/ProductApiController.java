package org.kosa.productcatalogservice.productcatalog.controller.product;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.kosa.productcatalogservice.productcatalog.dto.ApiResponse;
import org.kosa.productcatalogservice.productcatalog.dto.GuestCartItemDTO;
import org.kosa.productcatalogservice.productcatalog.dto.ProductDTO;
import org.kosa.productcatalogservice.productcatalog.dto.ProductDetailDTO;
import org.kosa.productcatalogservice.productcatalog.service.EnhancedProductService;
import org.kosa.productcatalogservice.productcatalog.service.ProductService;
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

    @GetMapping("/{productId}")
    public ResponseEntity<ProductDTO> getProductDetail(@PathVariable Integer productId) {
        try {
            log.info("상품 상세 조회 요청 - productId: {}", productId);

            ProductDTO product = productService.getProductById(productId);
            if (product == null) {
                log.warn("상품을 찾을 수 없음 - productId: {}", productId);
                return ResponseEntity.notFound().build();
            }

            productService.increaseViewCount(productId);
            log.info("상품 상세 조회 성공 - productId: {}", productId);
            return ResponseEntity.ok(product);
        } catch (Exception e) {
            log.error("상품 상세 조회 중 오류 - productId: {}", productId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

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

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductDTO>>> getAllProducts(
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
            return ResponseEntity.ok(ApiResponse.success(products));
        } catch (Exception e) {
            log.error("전체 상품 조회 실패 - error: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("상품 목록 조회 실패: " + e.getMessage()));
        }
    }

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

    @GetMapping("/recommended")
    public ResponseEntity<ApiResponse<List<ProductDTO>>> getRecommendedProducts(
            @RequestParam(required = false) String userId,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            log.info("추천 상품 조회 요청 - userId: {}, limit: {}", userId, limit);

            List<ProductDTO> products = enhancedProductService.getProductList(limit);

            log.info("추천 상품 조회 결과: {}개", products.size());
            return ResponseEntity.ok(ApiResponse.success(products));
        } catch (Exception e) {
            log.error("추천 상품 조회 실패 - userId: {}, error: {}", userId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("추천 상품 조회 실패: " + e.getMessage()));
        }
    }

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

    @GetMapping("/debug/status")
    public ResponseEntity<Map<String, Object>> getDatabaseStatus() {
        Map<String, Object> result = new HashMap<>();
        try {
            Long totalProducts = productService.getTotalActiveProductCount();
            result.put("totalActiveProducts", totalProducts);

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

    @PostMapping("/test-simple")
    public ResponseEntity<String> testSimple(@RequestBody String rawData) {
        log.info("받은 데이터: {}", rawData);
        return ResponseEntity.ok("OK");
    }
}