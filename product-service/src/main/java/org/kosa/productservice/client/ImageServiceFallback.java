package org.kosa.productservice.client;

import lombok.extern.slf4j.Slf4j;
import org.kosa.productservice.dto.ApiResponse;
import org.kosa.productservice.dto.ProductImageDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class ImageServiceFallback implements ImageServiceClient {

    @Override
    public ApiResponse<List<ProductImageDto>> getProductImages(String productId) {
        log.warn("이미지 서비스 연결 실패 - getProductImages: {}", productId);
        return ApiResponse.success("이미지 서비스 연결 실패", new ArrayList<>());
    }

    @Override
    public ApiResponse<ProductImageDto> getMainImage(String productId) {
        log.warn("이미지 서비스 연결 실패 - getMainImage: {}", productId);
        return ApiResponse.success("이미지 서비스 연결 실패", null);
    }

    @Override
    public ApiResponse<Map<String, ProductImageDto>> getMainImages(List<String> productIds) {
        log.warn("이미지 서비스 연결 실패 - getMainImages: {} 개", productIds.size());
        return ApiResponse.success("이미지 서비스 연결 실패", new HashMap<>());
    }

    @Override
    public ApiResponse<Map<String, List<ProductImageDto>>> getProductImages(List<String> productIds) {
        log.warn("이미지 서비스 연결 실패 - getProductImages: {} 개", productIds.size());
        return ApiResponse.success("이미지 서비스 연결 실패", new HashMap<>());
    }

    @Override
    public ApiResponse<Boolean> hasImages(String productId) {
        log.warn("이미지 서비스 연결 실패 - hasImages: {}", productId);
        return ApiResponse.success("이미지 서비스 연결 실패", false);
    }

    @Override
    public ApiResponse<Boolean> hasMainImage(String productId) {
        log.warn("이미지 서비스 연결 실패 - hasMainImage: {}", productId);
        return ApiResponse.success("이미지 서비스 연결 실패", false);
    }
}