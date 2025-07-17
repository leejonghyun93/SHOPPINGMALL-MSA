package org.kosa.commerceservice.controller.product;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kosa.commerceservice.dto.ApiResponse;
import org.kosa.commerceservice.dto.cart.GuestCartItemDTO;
import org.kosa.commerceservice.dto.product.ProductDTO;
import org.kosa.commerceservice.dto.product.ProductDetailDTO;
import org.kosa.commerceservice.service.product.EnhancedProductService;
import org.kosa.commerceservice.service.product.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.stream.Collectors;

@Tag(name = "상품 API", description = "상품 조회 및 관리 API")
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Slf4j
public class ProductApiController {

    private final ProductService productService;
    private final EnhancedProductService enhancedProductService;

    @Operation(summary = "상품 상세 조회", description = "상품 ID로 상세 정보를 조회합니다. 조회 시 조회수가 증가합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = ProductDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "상품을 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/{productId}")
    public ResponseEntity<ProductDTO> getProductDetail(
            @Parameter(description = "상품 ID", required = true, example = "1")
            @PathVariable Integer productId) {
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

    @Operation(summary = "상품 상세 조회 (이미지 포함)", description = "상품 ID로 이미지를 포함한 상세 정보를 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    @GetMapping("/{productId}/with-images")
    public ResponseEntity<ApiResponse<ProductDTO>> getProductDetailWithImages(
            @Parameter(description = "상품 ID", required = true, example = "1")
            @PathVariable Integer productId) {
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

    @Operation(summary = "전체 상품 목록 조회", description = "전체 상품 목록을 조회합니다. 이미지 포함 여부를 선택할 수 있습니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "조회 실패")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductDTO>>> getAllProducts(
            @Parameter(description = "조회할 상품 수", example = "20")
            @RequestParam(defaultValue = "20") int limit,
            @Parameter(description = "이미지 포함 여부", example = "false")
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

    @Operation(summary = "필터로 상품 조회", description = "카테고리 필터를 적용하여 상품을 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = ProductDTO.class)))
    })
    @GetMapping("/filter")
    public ResponseEntity<List<ProductDTO>> getProductsByFilter(
            @Parameter(description = "카테고리 ID (ALL: 전체)", example = "ALL")
            @RequestParam(name = "categoryId", defaultValue = "ALL") String categoryIdStr,
            @Parameter(description = "조회할 상품 수", example = "10")
            @RequestParam(name = "limit", defaultValue = "10") Integer limit,
            @Parameter(description = "이미지 포함 여부", example = "false")
            @RequestParam(name = "includeImages", defaultValue = "false") boolean includeImages) {
        try {
            // 디버깅: 실제 받은 파라미터들 로깅
            log.info("🔍 실제 받은 파라미터들:");
            log.info("  - categoryIdStr: '{}'", categoryIdStr);
            log.info("  - limit: {}", limit);
            log.info("  - includeImages: {}", includeImages);

            log.info("🔍 카테고리별 상품 조회 - categoryId: {}, limit: {}, includeImages: {}", categoryIdStr, limit, includeImages);

            List<ProductDTO> products = new ArrayList<>();

            if (includeImages) {
                if ("ALL".equals(categoryIdStr)) {
                    products = enhancedProductService.getProductList(limit);
                    log.info(" 전체 상품 조회 (이미지 포함): {}개", products.size());
                } else {
                    try {
                        Integer categoryId = Integer.parseInt(categoryIdStr);
                        log.info("🎯 파싱된 카테고리 ID: {}", categoryId);
                        products = enhancedProductService.getProductsByCategory(categoryId, limit);
                        log.info("✅카테고리 {} 상품 조회 (이미지 포함): {}개", categoryId, products.size());
                    } catch (NumberFormatException e) {
                        log.error("❌ 잘못된 카테고리 ID 형식: '{}', 에러: {}", categoryIdStr, e.getMessage());
                        return ResponseEntity.ok(List.of());
                    }
                }
            } else {
                if ("ALL".equals(categoryIdStr)) {
                    log.info("🌍 전체 상품 조회 실행");
                    products = productService.getAllProducts(limit);
                    log.info("✅ 전체 상품 조회: {}개", products.size());
                } else {
                    try {
                        Integer categoryId = Integer.parseInt(categoryIdStr);
                        log.info("🎯 파싱된 카테고리 ID: {} (원본: '{}')", categoryId, categoryIdStr);
                        products = productService.getProductsByCategory(categoryId, limit);
                        log.info("✅ 카테고리 {} 상품 조회: {}개", categoryId, products.size());

                        // 🔥 디버깅: 실제 반환된 상품들의 카테고리 확인
                        if (products.size() > 0) {
                            log.info("📊 반환된 상품들의 카테고리 분포:");
                            Map<Integer, Long> categoryDistribution = products.stream()
                                    .collect(Collectors.groupingBy(
                                            ProductDTO::getCategoryId,
                                            Collectors.counting()
                                    ));
                            categoryDistribution.forEach((catId, count) ->
                                    log.info("  - 카테고리 {}: {}개", catId, count)
                            );

                            // 🔥 요청한 카테고리와 일치하지 않는 상품이 있는지 확인
                            long mismatchCount = products.stream()
                                    .filter(p -> !p.getCategoryId().equals(categoryId))
                                    .count();

                            if (mismatchCount > 0) {
                                log.warn("⚠️ 요청한 카테고리 {}와 다른 카테고리 상품 {}개 발견!", categoryId, mismatchCount);

                                // 처음 5개의 잘못된 상품 로깅
                                products.stream()
                                        .filter(p -> !p.getCategoryId().equals(categoryId))
                                        .limit(5)
                                        .forEach(p -> log.warn("  - 잘못된 상품: ID={}, 이름={}, 카테고리={}",
                                                p.getProductId(), p.getName(), p.getCategoryId()));
                            } else {
                                log.info("✅ 모든 상품이 요청한 카테고리 {}와 일치합니다", categoryId);
                            }
                        }

                    } catch (NumberFormatException e) {
                        log.error("❌ 잘못된 카테고리 ID 형식: '{}', 에러: {}", categoryIdStr, e.getMessage());
                        return ResponseEntity.ok(List.of());
                    }
                }
            }

            log.info("🎯 최종 카테고리별 상품 조회 결과: {}개", products.size());
            return ResponseEntity.ok(products);

        } catch (Exception e) {
            log.error("❌ 카테고리별 상품 조회 중 오류:", e);
            return ResponseEntity.ok(List.of());
        }
    }

    @Operation(summary = "카테고리별 상품 조회", description = "특정 카테고리의 상품 목록을 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<ProductDTO>> getProductsByCategory(
            @Parameter(description = "카테고리 ID", required = true, example = "1")
            @PathVariable Integer categoryId,
            @Parameter(description = "조회할 상품 수", example = "20")
            @RequestParam(defaultValue = "20") int limit,
            @Parameter(description = "이미지 포함 여부", example = "false")
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

    private boolean isMainCategory(Integer categoryId) {
        if (categoryId == null) {
            return false;
        }
        // 메인 카테고리는 보통 1~9 또는 100단위
        // 실제 데이터에 맞게 조정 필요
        return categoryId < 100; // 100 미만은 메인 카테고리로 간주
    }

    @Operation(summary = "카테고리별 상품 조회 (이미지 포함)", description = "특정 카테고리의 상품을 이미지와 함께 조회합니다.")
    @GetMapping("/category/{categoryId}/with-images")
    public ResponseEntity<ApiResponse<List<ProductDTO>>> getProductsByCategoryWithImages(
            @Parameter(description = "카테고리 ID", required = true, example = "1")
            @PathVariable Integer categoryId,
            @Parameter(description = "조회할 상품 수", example = "20")
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

    @Operation(summary = "연관 상품 조회", description = "특정 상품과 연관된 상품 목록을 조회합니다.")
    @GetMapping("/{productId}/related")
    public ResponseEntity<List<ProductDTO>> getRelatedProducts(
            @Parameter(description = "상품 ID", required = true, example = "1")
            @PathVariable Integer productId,
            @Parameter(description = "조회할 상품 수", example = "4")
            @RequestParam(defaultValue = "4") int limit,
            @Parameter(description = "이미지 포함 여부", example = "false")
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

    @Operation(summary = "연관 상품 조회 (이미지 포함)", description = "특정 상품과 연관된 상품을 이미지와 함께 조회합니다.")
    @GetMapping("/{productId}/related/with-images")
    public ResponseEntity<ApiResponse<List<ProductDTO>>> getRelatedProductsWithImages(
            @Parameter(description = "상품 ID", required = true, example = "1")
            @PathVariable Integer productId,
            @Parameter(description = "조회할 상품 수", example = "4")
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

    @Operation(summary = "추천 상품 조회", description = "사용자에게 추천하는 상품 목록을 조회합니다.")
    @GetMapping("/recommended")
    public ResponseEntity<ApiResponse<List<ProductDTO>>> getRecommendedProducts(
            @Parameter(description = "사용자 ID", example = "user123")
            @RequestParam(required = false) String userId,
            @Parameter(description = "조회할 상품 수", example = "10")
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

    @Operation(summary = "호스트별 상품 조회", description = "특정 호스트(판매자)의 상품 목록을 조회합니다.")
    @GetMapping("/host/{hostId}")
    public ResponseEntity<List<ProductDTO>> getProductsByHost(
            @Parameter(description = "호스트 ID", required = true, example = "1")
            @PathVariable Long hostId,
            @Parameter(description = "조회할 상품 수", example = "20")
            @RequestParam(defaultValue = "20") int limit,
            @Parameter(description = "이미지 포함 여부", example = "false")
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

    @Operation(summary = "상품 개수 통계", description = "전체 또는 카테고리별 상품 개수를 조회합니다.")
    @GetMapping("/stats/count")
    public ResponseEntity<Map<String, Object>> getProductCount(
            @Parameter(description = "카테고리 ID (null 또는 ALL: 전체)", example = "ALL")
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

    @Operation(summary = "전체 카테고리별 상품 개수", description = "모든 카테고리의 상품 개수 통계를 조회합니다.")
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

    @Operation(summary = "데이터베이스 상태", description = "상품 데이터베이스의 상태를 확인합니다. (디버그용)")
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

    @Operation(summary = "게스트 장바구니 상품 상세", description = "게스트 장바구니에 담긴 상품들의 상세 정보를 조회합니다.")
    @PostMapping("/guest-cart-details")
    public ResponseEntity<List<ProductDetailDTO>> getGuestCartDetails(
            @Parameter(description = "장바구니 상품 목록", required = true)
            @RequestBody List<GuestCartItemDTO> cartItems) {
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

    @Operation(summary = "테스트 엔드포인트", description = "단순 테스트용 엔드포인트")
    @PostMapping("/test-simple")
    public ResponseEntity<String> testSimple(@RequestBody String rawData) {
        log.info("받은 데이터: {}", rawData);
        return ResponseEntity.ok("OK");
    }
}