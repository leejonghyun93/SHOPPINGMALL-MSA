package org.kosa.imageservice.service;


import lombok.RequiredArgsConstructor;
import org.kosa.imageservice.entity.ProductImage;
import org.kosa.imageservice.repository.ProductImageRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductImageService {

    private final ProductImageRepository imageRepository;

    public ProductImage saveImage(ProductImage image) {
        if (image.getImageId() == null || image.getImageId().isEmpty()) {
            image.setImageId(UUID.randomUUID().toString());
        }
        return imageRepository.save(image);
    }

    public List<ProductImage> getImagesByProductId(String productId) {
        return imageRepository.findByProductIdOrderByImageSeqAsc(productId);
    }

    public Optional<ProductImage> getMainImage(String productId) {
        return imageRepository.findByProductIdAndIsMainImage(productId, "Y");
    }

    public void deleteImage(String imageId) {
        imageRepository.deleteById(imageId);
    }
}