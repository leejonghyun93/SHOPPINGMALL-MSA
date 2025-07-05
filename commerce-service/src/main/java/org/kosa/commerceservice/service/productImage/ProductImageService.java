package org.kosa.commerceservice.service.productImage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.kosa.commerceservice.dto.productImage.ProductImageDto;
import org.kosa.commerceservice.entity.productImage.ProductImage;
import org.kosa.commerceservice.repository.productImage.ProductImageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductImageService {

    private final ProductImageRepository imageRepository;

    public ProductImage saveImage(ProductImage image) {
        if (image.getImageSeq() == null || image.getImageSeq() == 0) {
            List<ProductImage> existingImages = imageRepository.findByProductIdOrderByImageSeqAsc(image.getProductId());
            int nextSeq = existingImages.isEmpty() ? 1 :
                    existingImages.get(existingImages.size() - 1).getImageSeq() + 1;
            image.setImageSeq(nextSeq);
        }

        if (image.getStorageType() == null) {
            image.setStorageType("LOCAL");
        }
        if (image.getIsMainImage() == null) {
            image.setIsMainImage("N");
        }

        return imageRepository.save(image);
    }

    public List<ProductImage> getImagesByProductId(Integer productId) {
        return imageRepository.findByProductIdOrderByImageSeqAsc(productId);
    }

    public Optional<ProductImage> getMainImage(Integer productId) {
        return imageRepository.findByProductIdAndIsMainImage(productId, "Y");
    }

    public void deleteImage(Integer imageId) {
        imageRepository.deleteById(imageId);
    }

    @Transactional
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

    public void updateImageSequence(Integer imageId, Integer newSeq) {
        Optional<ProductImage> imageOpt = imageRepository.findById(imageId);
        if (imageOpt.isPresent()) {
            ProductImage image = imageOpt.get();
            image.setImageSeq(newSeq);
            imageRepository.save(image);
        }
    }

    public void deleteAllImagesByProductId(Integer productId) {
        List<ProductImage> images = getImagesByProductId(productId);
        imageRepository.deleteAll(images);
    }

    // API 응답용 메서드들
    public List<ProductImageDto> getProductImages(Integer productId) {
        return getImagesByProductId(productId).stream()
                .map(ProductImageDto::from)
                .collect(Collectors.toList());
    }

    public ProductImageDto getMainImageDto(Integer productId) {
        return getMainImage(productId)
                .map(ProductImageDto::from)
                .orElse(null);
    }

    public Map<String, ProductImageDto> getMainImages(List<Integer> productIds) {
        Map<String, ProductImageDto> result = new HashMap<>();
        for (Integer productId : productIds) {
            ProductImageDto mainImage = getMainImageDto(productId);
            if (mainImage != null) {
                result.put(productId.toString(), mainImage);
            }
        }
        return result;
    }

    public Map<String, List<ProductImageDto>> getProductImages(List<Integer> productIds) {
        Map<String, List<ProductImageDto>> result = new HashMap<>();
        for (Integer productId : productIds) {
            List<ProductImageDto> images = getProductImages(productId);
            result.put(productId.toString(), images);
        }
        return result;
    }

    public boolean hasImages(Integer productId) {
        return !getImagesByProductId(productId).isEmpty();
    }

    public boolean hasMainImage(Integer productId) {
        return getMainImage(productId).isPresent();
    }
}