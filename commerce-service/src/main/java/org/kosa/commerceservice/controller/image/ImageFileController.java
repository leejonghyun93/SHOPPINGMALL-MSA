package org.kosa.commerceservice.controller.image;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.nio.file.Path;
import java.nio.file.Paths;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Controller
@RequestMapping("/api/images")
@Slf4j
@Tag(name = "상품 이미지 파일", description = "상품 이미지 파일 서빙 관련 API")
public class ImageFileController {

    @Value("${image.upload.path:/uploads/images/}")
    private String uploadPath;

    /**
     *  상품 이미지 서빙
     *  요청 예시: GET /api/images/products/레몬.jpg
     */
    @GetMapping("/products/{fileName}")
    @Operation(summary = "상품 이미지 파일 서빙")
    public ResponseEntity<Resource> serveProductImage(
            @PathVariable String fileName,
            HttpServletRequest request) {

        try {
            log.info("상품 이미지 요청: {}", fileName);

            Path filePath = Paths.get(uploadPath, fileName);
            Resource resource = new UrlResource(filePath.toUri());

            log.info("파일 경로: {}", filePath.toAbsolutePath());
            log.info("파일 존재 여부: {}", resource.exists());
            log.info("파일 읽기 가능 여부: {}", resource.isReadable());

            if (resource.exists() && resource.isReadable()) {
                String contentType = determineContentType(fileName);

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000")
                        .header(HttpHeaders.ETAG, "\"" + resource.getFile().length() + "-" + resource.getFile().lastModified() + "\"")
                        .body(resource);
            } else {
                log.warn("이미지 파일을 찾을 수 없음: {} (경로: {})", fileName, filePath.toAbsolutePath());
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("이미지 파일 서빙 실패 - fileName: {}, error: {}", fileName, e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     *  썸네일 이미지 요청 시 thumb_ 접두어 붙여서 반환, 없으면 원본 대체
     */
    @GetMapping("/products/{fileName}/thumb")
    @Operation(summary = "상품 썸네일 이미지 서빙")
    public ResponseEntity<Resource> serveThumbnail(
            @PathVariable String fileName,
            HttpServletRequest request) {

        try {
            String thumbFileName = "thumb_" + fileName;
            Path filePath = Paths.get(uploadPath, thumbFileName);
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                String contentType = determineContentType(fileName);

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000")
                        .body(resource);
            } else {
                log.info("썸네일 파일이 없어 원본 이미지로 대체: {}", fileName);
                return serveProductImage(fileName, request);
            }
        } catch (Exception e) {
            log.error("썸네일 이미지 서빙 실패 - fileName: {}, error: {}", fileName, e.getMessage());
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
            default -> "application/octet-stream";
        };
    }
}
