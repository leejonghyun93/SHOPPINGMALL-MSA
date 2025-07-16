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

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
@Slf4j
public class ProductImageController {

    private final ProductImageService imageService;

    //  업로드 경로 설정 추가
    @Value("${image.upload.path:./dev-uploads/images/}")
    private String uploadPath;

    @PostMapping
    public ResponseEntity<ProductImage> uploadImage(@RequestBody ProductImage image) {
        return ResponseEntity.ok(imageService.saveImage(image));
    }

    @GetMapping("/metadata/{productId}")
    public ResponseEntity<List<ProductImage>> getImages(@PathVariable Integer productId) {
        return ResponseEntity.ok(imageService.getImagesByProductId(productId));
    }

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

    //  업로드된 상품 메인 이미지 서빙
    @GetMapping("/upload/product/main/{fileName}")
    public ResponseEntity<Resource> serveUploadProductMainImage(
            @PathVariable String fileName,
            HttpServletRequest request) {

        try {
            log.info("업로드 상품 메인 이미지 요청: {}", fileName);

            // 실제 파일 경로 찾기
            Path filePath = Paths.get(uploadPath, "upload", "product", "main", fileName);
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                String contentType = determineContentType(fileName);

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000")
                        .body(resource);
            } else {
                log.warn("업로드 상품 이미지 파일을 찾을 수 없음: {}", filePath.toAbsolutePath());
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("업로드 상품 이미지 서빙 실패 - fileName: {}, error: {}", fileName, e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    // 범용 업로드 이미지 핸들러
    @GetMapping("/upload/**")
    public ResponseEntity<Resource> serveUploadImage(HttpServletRequest request) {
        try {
            String requestURI = request.getRequestURI();
            // 🔥 /api/images 제거하여 실제 파일 경로 추출
            String requestPath = requestURI.replace("/api/images", "");

            log.info("업로드 이미지 요청 - 원본 URI: {}", requestURI);
            log.info("업로드 이미지 요청 - 추출된 경로: {}", requestPath);

            // 🔥 경로 정규화
            if (requestPath.startsWith("/")) {
                requestPath = requestPath.substring(1);
            }

            log.info("업로드 이미지 요청 - 정규화된 경로: {}", requestPath);

            // 🔥 실제 파일 경로 구성
            Path filePath = Paths.get(uploadPath, requestPath);
            log.info("업로드 이미지 요청 - 최종 파일 경로: {}", filePath.toAbsolutePath());

            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                String fileName = filePath.getFileName().toString();
                String contentType = determineContentType(fileName);

                log.info("이미지 파일 찾음: {}", filePath.toAbsolutePath());

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000")
                        .body(resource);
            } else {
                log.warn("업로드 이미지 파일을 찾을 수 없음: {}", filePath.toAbsolutePath());

                // 🔥 파일이 없을 때 디렉토리 구조 출력 (디버깅용)
                Path uploadDir = Paths.get(uploadPath);
                if (Files.exists(uploadDir)) {
                    log.info("업로드 디렉토리 존재함: {}", uploadDir.toAbsolutePath());
                    try (Stream<Path> paths = Files.walk(uploadDir, 3)) {
                        paths.filter(Files::isRegularFile)
                                .limit(10)
                                .forEach(path -> log.info("발견된 파일: {}", path.toAbsolutePath()));
                    }
                } else {
                    log.warn("업로드 디렉토리가 존재하지 않음: {}", uploadDir.toAbsolutePath());
                }

                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("업로드 이미지 서빙 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }


    //  파일 확장자에 따른 Content-Type 결정
    private String determineContentType(String fileName) {
        String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();

        switch (extension) {
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            case "webp":
                return "image/webp";
            case "svg":
                return "image/svg+xml";
            default:
                return "image/jpeg";
        }
    }
}