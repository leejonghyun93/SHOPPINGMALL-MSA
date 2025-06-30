package org.kosa.imageservice.service;

import lombok.RequiredArgsConstructor;
import org.kosa.imageservice.entity.ProductImage;
import org.kosa.imageservice.repository.ProductImageRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductImageService {

    private final ProductImageRepository imageRepository;

    public ProductImage saveImage(ProductImage image) {
        // imageId는 AUTO_INCREMENT이므로 설정하지 않음

        // imageSeq가 설정되지 않은 경우 기본값 설정
        if (image.getImageSeq() == null || image.getImageSeq() == 0) {
            // 해당 상품의 마지막 순서 + 1로 설정
            List<ProductImage> existingImages = imageRepository.findByProductIdOrderByImageSeqAsc(image.getProductId());
            int nextSeq = existingImages.isEmpty() ? 1 :
                    existingImages.get(existingImages.size() - 1).getImageSeq() + 1;
            image.setImageSeq(nextSeq);
        }

        // 기본값들 설정
        if (image.getStorageType() == null) {
            image.setStorageType("LOCAL");
        }
        if (image.getIsMainImage() == null) {
            image.setIsMainImage("N");
        }

        return imageRepository.save(image);
    }

    public List<ProductImage> getImagesByProductId(Integer productId) {  // String → Integer
        return imageRepository.findByProductIdOrderByImageSeqAsc(productId);
    }

    public Optional<ProductImage> getMainImage(Integer productId) {  // String → Integer
        return imageRepository.findByProductIdAndIsMainImage(productId, "Y");
    }

    public void deleteImage(Integer imageId) {  // String → Integer
        imageRepository.deleteById(imageId);
    }

    // 메인 이미지 설정
    public ProductImage setMainImage(Integer productId, Integer imageId) {
        // 기존 메인 이미지 해제
        List<ProductImage> allImages = getImagesByProductId(productId);
        for (ProductImage img : allImages) {
            if ("Y".equals(img.getIsMainImage())) {
                img.setIsMainImage("N");
                imageRepository.save(img);
            }
        }

        // 새로운 메인 이미지 설정
        Optional<ProductImage> targetImage = imageRepository.findById(imageId);
        if (targetImage.isPresent()) {
            ProductImage image = targetImage.get();
            image.setIsMainImage("Y");
            return imageRepository.save(image);
        }

        throw new IllegalArgumentException("Image not found: " + imageId);
    }

    // 이미지 순서 변경
    public void updateImageSequence(Integer imageId, Integer newSeq) {
        Optional<ProductImage> imageOpt = imageRepository.findById(imageId);
        if (imageOpt.isPresent()) {
            ProductImage image = imageOpt.get();
            image.setImageSeq(newSeq);
            imageRepository.save(image);
        }
    }

    // 상품의 모든 이미지 삭제
    public void deleteAllImagesByProductId(Integer productId) {
        List<ProductImage> images = getImagesByProductId(productId);
        imageRepository.deleteAll(images);
    }
}