package org.kosa.productservice.config;

import org.springframework.stereotype.Component;

/**
 * 캐시 키 생성 유틸리티 클래스
 */
@Component
public class CacheKeyGenerator {

    public static final String PRODUCT_DETAIL_PREFIX = "product:detail:";
    public static final String PRODUCT_LIST_PREFIX = "product:list:";
    public static final String PRODUCT_CATEGORY_PREFIX = "product:category:";
    public static final String RELATED_PRODUCTS_PREFIX = "product:related:";
    public static final String PRODUCTS_BY_HOST_PREFIX = "product:host:";
    public static final String PRODUCTS_IN_STOCK_PREFIX = "product:stock:";
    public static final String DISCOUNTED_PRODUCTS_PREFIX = "product:discount:";
    public static final String POPULAR_PRODUCTS_PREFIX = "product:popular:";

    public String generateProductDetailKey(Integer productId) {
        return PRODUCT_DETAIL_PREFIX + productId;
    }

    public String generateProductListKey(int limit, int offset) {
        return PRODUCT_LIST_PREFIX + limit + ":" + offset;
    }

    public String generateCategoryProductsKey(Integer categoryId, int limit) {
        return PRODUCT_CATEGORY_PREFIX + categoryId + ":" + limit;
    }

    public String generateRelatedProductsKey(Integer productId, int limit) {
        return RELATED_PRODUCTS_PREFIX + productId + ":" + limit;
    }

    public String generateHostProductsKey(Long hostId, int limit) {
        return PRODUCTS_BY_HOST_PREFIX + hostId + ":" + limit;
    }

    public String generateStockProductsKey(int limit) {
        return PRODUCTS_IN_STOCK_PREFIX + limit;
    }

    public String generateDiscountProductsKey(int limit) {
        return DISCOUNTED_PRODUCTS_PREFIX + limit;
    }

    public String generatePopularProductsKey(int limit) {
        return POPULAR_PRODUCTS_PREFIX + limit;
    }

    // 하위 호환성을 위한 String 오버로드 메서드들
    public String generateProductDetailKey(String productId) {
        return PRODUCT_DETAIL_PREFIX + productId;
    }

    public String generateCategoryProductsKey(String categoryId, int limit) {
        return PRODUCT_CATEGORY_PREFIX + categoryId + ":" + limit;
    }

    public String generateRelatedProductsKey(String productId, int limit) {
        return RELATED_PRODUCTS_PREFIX + productId + ":" + limit;
    }
}