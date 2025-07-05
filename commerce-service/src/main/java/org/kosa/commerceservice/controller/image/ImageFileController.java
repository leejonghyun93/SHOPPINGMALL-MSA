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

@Controller
@RequestMapping("/api/images")
@Slf4j
public class ImageFileController {

    @Value("${image.upload.path:/uploads/images/}")
    private String uploadPath;

    @GetMapping("/products/{fileName}")
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

                log.info("이미지 서빙 성공: {} (타입: {})", fileName, contentType);

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

    @GetMapping("/../../images/{fileName}")
    public ResponseEntity<Resource> serveLegacyImage(
            @PathVariable String fileName,
            HttpServletRequest request) {

        log.info("레거시 이미지 경로 사용: {}", fileName);
        return serveProductImage(fileName, request);
    }

    @GetMapping("/products/{fileName}/thumb")
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
            default:
                return "image/jpeg";
        }
    }
}
