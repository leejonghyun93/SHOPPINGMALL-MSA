package org.kosa.productservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 🔥 캐시 관리를 위한 컨트롤러
 * 개발/운영 환경에서 캐시 상태 확인 및 관리용
 */
@RestController
@RequestMapping("/api/cache")
@RequiredArgsConstructor
@Slf4j
public class CacheManagementController {

    private final CacheManager cacheManager;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 🔥 캐시 통계 조회
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getCacheStatistics() {
        try {
            Map<String, Object> stats = new HashMap<>();

            // 캐시 키 개수 조회
            stats.put("productDetailCaches", countCacheKeys("productDetail::*"));
            stats.put("productListCaches", countCacheKeys("productList::*"));
            stats.put("categoryProductCaches", countCacheKeys("productsByCategory::*"));
            stats.put("relatedProductCaches", countCacheKeys("relatedProducts::*"));
            stats.put("categoryCaches", countCacheKeys("categories::*"));

            // 추가 Redis 정보
            stats.put("totalKeys", getTotalKeyCount());
            stats.put("cacheNames", getCacheNames());
            stats.put("timestamp", new Date());

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("캐시 통계 조회 실패", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "캐시 통계 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * 🔥 특정 상품 캐시 삭제
     */
    @DeleteMapping("/product/{productId}")
    public ResponseEntity<Map<String, Object>> evictProductCache(@PathVariable String productId) {
        try {
            // 상품 관련 캐시 직접 삭제
            String productDetailKey = "productDetail::" + productId;
            redisTemplate.delete(productDetailKey);

            // 관련 상품 캐시도 삭제 (해당 상품이 포함된 모든 관련 상품 캐시)
            Set<String> relatedKeys = redisTemplate.keys("relatedProducts::*");
            if (relatedKeys != null) {
                redisTemplate.delete(relatedKeys);
            }

            Map<String, Object> response = Map.of(
                    "success", true,
                    "message", "상품 캐시 삭제 완료: " + productId,
                    "timestamp", new Date()
            );

            log.info("🗑️ 상품 캐시 삭제: {}", productId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("상품 캐시 삭제 실패: {}", productId, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "캐시 삭제 실패: " + e.getMessage()));
        }
    }

    /**
     * 🔥 특정 카테고리 캐시 삭제
     */
    @DeleteMapping("/category/{categoryId}")
    public ResponseEntity<Map<String, Object>> evictCategoryCache(@PathVariable String categoryId) {
        try {
            // 카테고리별 상품 캐시 삭제
            Set<String> categoryKeys = redisTemplate.keys("productsByCategory::" + categoryId + "*");
            if (categoryKeys != null && !categoryKeys.isEmpty()) {
                redisTemplate.delete(categoryKeys);
                log.info("🗑️ 카테고리 상품 캐시 삭제: {} ({}개)", categoryId, categoryKeys.size());
            }

            // 카테고리 정보 캐시 삭제
            Set<String> categoryInfoKeys = redisTemplate.keys("categories::*" + categoryId + "*");
            if (categoryInfoKeys != null && !categoryInfoKeys.isEmpty()) {
                redisTemplate.delete(categoryInfoKeys);
                log.info("🗑️ 카테고리 정보 캐시 삭제: {} ({}개)", categoryId, categoryInfoKeys.size());
            }

            Map<String, Object> response = Map.of(
                    "success", true,
                    "message", "카테고리 캐시 삭제 완료: " + categoryId,
                    "deletedProductCaches", categoryKeys != null ? categoryKeys.size() : 0,
                    "deletedCategoryCaches", categoryInfoKeys != null ? categoryInfoKeys.size() : 0,
                    "timestamp", new Date()
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("카테고리 캐시 삭제 실패: {}", categoryId, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "캐시 삭제 실패: " + e.getMessage()));
        }
    }

    /**
     * 🔥 모든 상품 캐시 삭제
     */
    @DeleteMapping("/product/all")
    public ResponseEntity<Map<String, Object>> evictAllProductCaches() {
        try {
            // 모든 상품 관련 캐시 삭제
            Set<String> productKeys = redisTemplate.keys("product*");
            int deletedCount = 0;

            if (productKeys != null && !productKeys.isEmpty()) {
                redisTemplate.delete(productKeys);
                deletedCount = productKeys.size();
                log.info("🗑️ 모든 상품 캐시 삭제: {}개", deletedCount);
            }

            Map<String, Object> response = Map.of(
                    "success", true,
                    "message", "모든 상품 캐시 삭제 완료",
                    "deletedCount", deletedCount,
                    "timestamp", new Date()
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("전체 상품 캐시 삭제 실패", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "캐시 삭제 실패: " + e.getMessage()));
        }
    }

    /**
     * 🔥 모든 캐시 삭제 (주의: 운영환경에서 신중히 사용)
     */
    @DeleteMapping("/all")
    public ResponseEntity<Map<String, Object>> evictAllCaches() {
        try {
            // Spring Cache Manager를 통한 캐시 삭제
            cacheManager.getCacheNames().forEach(cacheName -> {
                Objects.requireNonNull(cacheManager.getCache(cacheName)).clear();
                log.info("캐시 삭제: {}", cacheName);
            });

            Map<String, Object> response = Map.of(
                    "success", true,
                    "message", "모든 캐시 삭제 완료",
                    "clearedCaches", cacheManager.getCacheNames(),
                    "timestamp", new Date()
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("전체 캐시 삭제 실패", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "캐시 삭제 실패: " + e.getMessage()));
        }
    }

    /**
     * 🔥 캐시 Warm-up (주요 데이터 미리 캐싱) - MSA 환경에서는 제거
     * 실제로는 각 서비스의 주요 API를 호출하여 캐시를 생성해야 함
     */
    @PostMapping("/warmup")
    public ResponseEntity<Map<String, Object>> warmUpCaches() {
        try {
            log.info("🔥 캐시 Warm-up 시작 (MSA 환경)");

            Map<String, Object> response = Map.of(
                    "success", true,
                    "message", "MSA 환경에서는 각 API 호출을 통해 캐시를 생성하세요",
                    "recommendation", List.of(
                            "GET /api/products?limit=20 (전체 상품)",
                            "GET /api/products/category/1?limit=20 (카테고리별)",
                            "GET /api/products/category/ALL?limit=50 (전체)",
                            "GET /api/categories (카테고리 목록)"
                    ),
                    "timestamp", new Date()
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("캐시 Warm-up 실패", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Warm-up 실패: " + e.getMessage()));
        }
    }

    /**
     * 🔥 Redis 키 목록 조회 (디버깅용)
     */
    @GetMapping("/keys")
    public ResponseEntity<Map<String, Object>> getRedisKeys(
            @RequestParam(defaultValue = "*") String pattern,
            @RequestParam(defaultValue = "100") int limit) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            List<String> keyList = keys != null ?
                    keys.stream().limit(limit).sorted().toList() :
                    new ArrayList<>();

            Map<String, Object> response = Map.of(
                    "pattern", pattern,
                    "totalCount", keys != null ? keys.size() : 0,
                    "returnedCount", keyList.size(),
                    "keys", keyList,
                    "timestamp", new Date()
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Redis 키 조회 실패", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "키 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * 🔥 캐시 히트율 조회 (간단한 버전)
     */
    @GetMapping("/hit-rate")
    public ResponseEntity<Map<String, Object>> getCacheHitRate() {
        try {
            Map<String, Object> hitRateInfo = new HashMap<>();

            // Spring Cache의 기본 통계는 제한적이므로 간단한 정보만 제공
            hitRateInfo.put("message", "상세한 히트율 정보는 Redis 모니터링 도구 사용 권장");
            hitRateInfo.put("cacheNames", getCacheNames());
            hitRateInfo.put("totalKeys", getTotalKeyCount());
            hitRateInfo.put("timestamp", new Date());

            return ResponseEntity.ok(hitRateInfo);
        } catch (Exception e) {
            log.error("캐시 히트율 조회 실패", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "히트율 조회 실패: " + e.getMessage()));
        }
    }

    // === 유틸리티 메서드들 ===

    private long countCacheKeys(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            return keys != null ? keys.size() : 0;
        } catch (Exception e) {
            return -1;
        }
    }

    private long getTotalKeyCount() {
        try {
            Set<String> keys = redisTemplate.keys("*");
            return keys != null ? keys.size() : 0;
        } catch (Exception e) {
            log.warn("Redis 키 개수 조회 실패", e);
            return -1;
        }
    }

    private Collection<String> getCacheNames() {
        try {
            return cacheManager.getCacheNames();
        } catch (Exception e) {
            log.warn("캐시 이름 조회 실패", e);
            return Collections.emptyList();
        }
    }
}