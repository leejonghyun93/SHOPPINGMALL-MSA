package org.kosa.productservice.client;

import org.kosa.productservice.dto.ApiResponse;
import org.kosa.productservice.dto.ProductImageDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

@FeignClient(name = "image-service", url = "${services.image-service.url}", fallback = ImageServiceFallback.class)
public interface ImageServiceClient {

    @GetMapping("/api/images/products/{productId}")
    ApiResponse<List<ProductImageDto>> getProductImages(@PathVariable String productId);

    @GetMapping("/api/images/products/{productId}/main")
    ApiResponse<ProductImageDto> getMainImage(@PathVariable String productId);

    @PostMapping("/api/images/products/main")
    ApiResponse<Map<String, ProductImageDto>> getMainImages(@RequestBody List<String> productIds);

    @PostMapping("/api/images/products/all")
    ApiResponse<Map<String, List<ProductImageDto>>> getProductImages(@RequestBody List<String> productIds);

    @GetMapping("/api/images/products/{productId}/exists")
    ApiResponse<Boolean> hasImages(@PathVariable String productId);

    @GetMapping("/api/images/products/{productId}/main/exists")
    ApiResponse<Boolean> hasMainImage(@PathVariable String productId);
}