package org.kosa.imageservice.controller;

import lombok.RequiredArgsConstructor;
import org.kosa.imageservice.entity.ProductImage;
import org.kosa.imageservice.service.ProductImageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ProductImageController {

    private final ProductImageService imageService;

    @PostMapping
    public ResponseEntity<ProductImage> uploadImage(@RequestBody ProductImage image) {
        return ResponseEntity.ok(imageService.saveImage(image));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<List<ProductImage>> getImages(@PathVariable String productId) {
        return ResponseEntity.ok(imageService.getImagesByProductId(productId));
    }

    @GetMapping("/{productId}/main")
    public ResponseEntity<ProductImage> getMainImage(@PathVariable String productId) {
        return imageService.getMainImage(productId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{imageId}")
    public ResponseEntity<Void> deleteImage(@PathVariable String imageId) {
        imageService.deleteImage(imageId);
        return ResponseEntity.ok().build();
    }
}