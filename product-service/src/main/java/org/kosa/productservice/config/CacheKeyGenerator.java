package org.kosa.productservice.config;

import org.springframework.stereotype.Component;

/**
 * 캐시 키 생성 유틸리티 클래스
 */
@Component
class CacheKeyGenerator {

    public static final String PRODUCT_DETAIL_PREFIX = "product:detail:";
    public static final String PRODUCT_LIST_PREFIX = "product:list:";
    public static final String PRODUCT_CATEGORY_PREFIX = "product:category:";
    public static final String RELATED_PRODUCTS_PREFIX = "product:related:";

    public String generateProductDetailKey(String productId) {
        return PRODUCT_DETAIL_PREFIX + productId;
    }

    public String generateProductListKey(int limit, int offset) {
        return PRODUCT_LIST_PREFIX + limit + ":" + offset;
    }

    public String generateCategoryProductsKey(String categoryId, int limit) {
        return PRODUCT_CATEGORY_PREFIX + categoryId + ":" + limit;
    }

    public String generateRelatedProductsKey(String productId, int limit) {
        return RELATED_PRODUCTS_PREFIX + productId + ":" + limit;
    }
}