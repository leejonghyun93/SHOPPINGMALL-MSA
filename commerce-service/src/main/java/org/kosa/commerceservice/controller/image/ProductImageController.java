package org.kosa.commerceservice.controller.image;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.kosa.commerceservice.dto.ApiResponse;
import org.kosa.commerceservice.dto.productImage.ProductImageDto;
import org.kosa.commerceservice.entity.productImage.ProductImage;
import org.kosa.commerceservice.service.productImage.ProductImageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
@Slf4j
public class ProductImageController {

    private final ProductImageService imageService;

    @PostMapping
    public ResponseEntity<ProductImage> uploadImage(@RequestBody ProductImage image) {
        return ResponseEntity.ok(imageService.saveImage(image));
    }

    // 🔥 이 매핑을 제거하거나 변경 (파일 서빙과 충돌)
    // @GetMapping("/{productId}")

    // 🔥 대신 더 구체적인 매핑 사용
    @GetMapping("/metadata/{productId}")
    public ResponseEntity<List<ProductImage>> getImages(@PathVariable Integer productId) {
        return ResponseEntity.ok(imageService.getImagesByProductId(productId));
    }

    // 🔥 이것도 변경
    @GetMapping("/metadata/{productId}/main")
    public ResponseEntity<ProductImage> getMainImage(@PathVariable Integer productId) {
        return imageService.getMainImage(productId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{imageId}")
    public ResponseEntity<Void> deleteImage(@PathVariable Integer imageId) {
        imageService.deleteImage(imageId);
        return ResponseEntity.ok().build();
    }

    // 나머지 기존 매핑들은 그대로 유지 (이미 /products/로 시작하므로 괜찮음)
    @GetMapping("/products/{productId}")
    public ResponseEntity<ApiResponse<List<ProductImageDto>>> getProductImages(@PathVariable String productId) {
        try {
            Integer id = Integer.parseInt(productId);
            List<ProductImageDto> images = imageService.getProductImages(id);
            return ResponseEntity.ok(ApiResponse.success(images));
        } catch (Exception e) {
            log.error("상품 이미지 조회 실패: {}", productId, e);
            return ResponseEntity.ok(ApiResponse.error("이미지 조회 실패"));
        }
    }

    @GetMapping("/products/{productId}/main")
    public ResponseEntity<ApiResponse<ProductImageDto>> getProductMainImage(@PathVariable String productId) {
        try {
            Integer id = Integer.parseInt(productId);
            ProductImageDto mainImage = imageService.getMainImageDto(id);
            return ResponseEntity.ok(ApiResponse.success(mainImage));
        } catch (Exception e) {
            log.error("메인 이미지 조회 실패: {}", productId, e);
            return ResponseEntity.ok(ApiResponse.error("메인 이미지 조회 실패"));
        }
    }

    @PostMapping("/products/main")
    public ResponseEntity<ApiResponse<Map<String, ProductImageDto>>> getMainImages(@RequestBody List<String> productIds) {
        try {
            List<Integer> ids = productIds.stream().map(Integer::parseInt).collect(java.util.stream.Collectors.toList());
            Map<String, ProductImageDto> mainImages = imageService.getMainImages(ids);
            return ResponseEntity.ok(ApiResponse.success(mainImages));
        } catch (Exception e) {
            log.error("메인 이미지 목록 조회 실패", e);
            return ResponseEntity.ok(ApiResponse.error("메인 이미지 목록 조회 실패"));
        }
    }

    @PostMapping("/products/all")
    public ResponseEntity<ApiResponse<Map<String, List<ProductImageDto>>>> getProductImages(@RequestBody List<String> productIds) {
        try {
            List<Integer> ids = productIds.stream().map(Integer::parseInt).collect(java.util.stream.Collectors.toList());
            Map<String, List<ProductImageDto>> allImages = imageService.getProductImages(ids);
            return ResponseEntity.ok(ApiResponse.success(allImages));
        } catch (Exception e) {
            log.error("상품 이미지 목록 조회 실패", e);
            return ResponseEntity.ok(ApiResponse.error("상품 이미지 목록 조회 실패"));
        }
    }

    @GetMapping("/products/{productId}/exists")
    public ResponseEntity<ApiResponse<Boolean>> hasImages(@PathVariable String productId) {
        try {
            Integer id = Integer.parseInt(productId);
            boolean hasImages = imageService.hasImages(id);
            return ResponseEntity.ok(ApiResponse.success(hasImages));
        } catch (Exception e) {
            log.error("이미지 존재 여부 확인 실패: {}", productId, e);
            return ResponseEntity.ok(ApiResponse.success(false));
        }
    }

    @GetMapping("/products/{productId}/main/exists")
    public ResponseEntity<ApiResponse<Boolean>> hasMainImage(@PathVariable String productId) {
        try {
            Integer id = Integer.parseInt(productId);
            boolean hasMainImage = imageService.hasMainImage(id);
            return ResponseEntity.ok(ApiResponse.success(hasMainImage));
        } catch (Exception e) {
            log.error("메인 이미지 존재 여부 확인 실패: {}", productId, e);
            return ResponseEntity.ok(ApiResponse.success(false));
        }
    }
}