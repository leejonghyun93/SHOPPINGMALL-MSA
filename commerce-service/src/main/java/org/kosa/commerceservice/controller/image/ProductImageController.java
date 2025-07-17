package org.kosa.commerceservice.controller.image;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kosa.commerceservice.dto.ApiResponse;
import org.kosa.commerceservice.dto.productImage.ProductImageDto;
import org.kosa.commerceservice.entity.productImage.ProductImage;
import org.kosa.commerceservice.service.productImage.ProductImageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "상품 이미지", description = "상품 이미지 관련 API")
public class ProductImageController {

    private final ProductImageService imageService;

    @Value("${image.upload.path:./dev-uploads/images/}")
    private String uploadPath;

    @PostMapping
    @Operation(summary = "상품 이미지 등록")
    public ResponseEntity<ProductImage> uploadImage(@RequestBody ProductImage image) {
        return ResponseEntity.ok(imageService.saveImage(image));
    }

    @GetMapping("/metadata/{productId}")
    @Operation(summary = "상품 이미지 메타데이터 조회")
    public ResponseEntity<List<ProductImage>> getImages(@PathVariable Integer productId) {
        return ResponseEntity.ok(imageService.getImagesByProductId(productId));
    }

    @GetMapping("/metadata/{productId}/main")
    @Operation(summary = "상품 메인 이미지 메타데이터 조회")
    public ResponseEntity<ProductImage> getMainImage(@PathVariable Integer productId) {
        return imageService.getMainImage(productId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{imageId}")
    @Operation(summary = "상품 이미지 삭제")
    public ResponseEntity<Void> deleteImage(@PathVariable Integer imageId) {
        imageService.deleteImage(imageId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/products/{productId}")
    @Operation(summary = "상품 이미지 조회")
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
    @Operation(summary = "상품 메인 이미지 조회")
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
    @Operation(summary = "다수 상품 메인 이미지 조회")
    public ResponseEntity<ApiResponse<Map<String, ProductImageDto>>> getMainImages(@RequestBody List<String> productIds) {
        try {
            List<Integer> ids = productIds.stream().map(Integer::parseInt).toList();
            Map<String, ProductImageDto> mainImages = imageService.getMainImages(ids);
            return ResponseEntity.ok(ApiResponse.success(mainImages));
        } catch (Exception e) {
            log.error("메인 이미지 목록 조회 실패", e);
            return ResponseEntity.ok(ApiResponse.error("메인 이미지 목록 조회 실패"));
        }
    }

    @PostMapping("/products/all")
    @Operation(summary = "다수 상품 이미지 조회")
    public ResponseEntity<ApiResponse<Map<String, List<ProductImageDto>>>> getProductImages(@RequestBody List<String> productIds) {
        try {
            List<Integer> ids = productIds.stream().map(Integer::parseInt).toList();
            Map<String, List<ProductImageDto>> allImages = imageService.getProductImages(ids);
            return ResponseEntity.ok(ApiResponse.success(allImages));
        } catch (Exception e) {
            log.error("상품 이미지 목록 조회 실패", e);
            return ResponseEntity.ok(ApiResponse.error("상품 이미지 목록 조회 실패"));
        }
    }

    @GetMapping("/products/{productId}/exists")
    @Operation(summary = "상품 이미지 존재 여부 확인")
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
    @Operation(summary = "상품 메인 이미지 존재 여부 확인")
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

    @GetMapping("/upload/product/main/{fileName}")
    @Operation(summary = "상품 메인 이미지 파일 서빙")
    public ResponseEntity<Resource> serveUploadProductMainImage(@PathVariable String fileName, HttpServletRequest request) {
        try {
            Path filePath = Paths.get(uploadPath, "upload", "product", "main", fileName);
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                String contentType = determineContentType(fileName);
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("업로드 상품 이미지 서빙 실패 - fileName: {}, error: {}", fileName, e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/upload/**")
    @Operation(summary = "업로드된 이미지 범용 서빙")
    public ResponseEntity<Resource> serveUploadImage(HttpServletRequest request) {
        try {
            String requestPath = request.getRequestURI().replace("/api/images", "");
            if (requestPath.startsWith("/")) {
                requestPath = requestPath.substring(1);
            }
            Path filePath = Paths.get(uploadPath, requestPath);
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                String fileName = filePath.getFileName().toString();
                String contentType = determineContentType(fileName);
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("업로드 이미지 서빙 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    private String determineContentType(String fileName) {
        String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        return switch (extension) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "svg" -> "image/svg+xml";
            default -> "application/octet-stream";
        };
    }
}
