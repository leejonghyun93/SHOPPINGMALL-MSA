package org.kosa.commerceservice.service.productImage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.kosa.commerceservice.dto.productImage.ProductImageDto;
import org.kosa.commerceservice.entity.productImage.ProductImage;
import org.kosa.commerceservice.repository.productImage.ProductImageRepository;
import org.springframework.beans.factory.annotation.Value;
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

    //  환경별 설정 주입
    @Value("${image.base-url:}")
    private String baseUrl;

    @Value("${server.domain:localhost}")
    private String serverDomain;

    @Value("${server.port:8080}")
    private String serverPort;

    @Value("${server.protocol:http}")
    private String serverProtocol;

    //  완전한 URL 생성 메서드
    private String buildCompleteUrl(String relativePath) {
        if (relativePath == null || relativePath.trim().isEmpty()) {
            return null;
        }

        // 이미 완전한 URL인 경우
        if (relativePath.startsWith("http://") || relativePath.startsWith("https://")) {
            return relativePath;
        }

        // baseUrl이 설정된 경우 사용 (우선순위)
        if (baseUrl != null && !baseUrl.trim().isEmpty()) {
            if (relativePath.startsWith("/api/images/products/")) {
                return baseUrl.replace("/api/images/products/", "") + relativePath;
            }
            return baseUrl + (relativePath.startsWith("/") ? relativePath.substring(1) : relativePath);
        }

        // 동적으로 URL 생성
        String domain = "localhost".equals(serverDomain) ? "localhost" : serverDomain;
        String port = ("80".equals(serverPort) || "443".equals(serverPort)) ? "" : ":" + serverPort;

        return String.format("%s://%s%s%s",
                serverProtocol,
                domain,
                port,
                relativePath.startsWith("/") ? relativePath : "/" + relativePath
        );
    }

    //  ProductImageDto에 완전한 URL 설정
    private ProductImageDto enrichWithCompleteUrl(ProductImageDto dto) {
        if (dto != null && dto.getImageUrl() != null) {
            String completeUrl = buildCompleteUrl(dto.getImageUrl());
            dto.setImageUrl(completeUrl);
            log.debug("이미지 URL 변환: {} -> {}", dto.getImageUrl(), completeUrl);
        }
        return dto;
    }

    //  ProductImageDto 리스트에 완전한 URL 설정
    private List<ProductImageDto> enrichWithCompleteUrls(List<ProductImageDto> dtos) {
        return dtos.stream()
                .map(this::enrichWithCompleteUrl)
                .collect(Collectors.toList());
    }

    // ========== 기존 엔티티 관련 메서드들 ==========

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

        log.info("이미지 저장: productId={}, fileName={}, isMainImage={}",
                image.getProductId(), image.getFileName(), image.getIsMainImage());

        return imageRepository.save(image);
    }

    public List<ProductImage> getImagesByProductId(Integer productId) {
        List<ProductImage> images = imageRepository.findByProductIdOrderByImageSeqAsc(productId);
        log.debug("상품 {}의 이미지 {}개 조회", productId, images.size());
        return images;
    }

    public Optional<ProductImage> getMainImage(Integer productId) {
        Optional<ProductImage> mainImage = imageRepository.findByProductIdAndIsMainImage(productId, "Y");
        log.debug("상품 {}의 메인 이미지 조회: {}", productId, mainImage.isPresent() ? "존재" : "없음");
        return mainImage;
    }

    public void deleteImage(Integer imageId) {
        log.info("이미지 삭제: imageId={}", imageId);
        imageRepository.deleteById(imageId);
    }

    @Transactional
    public ProductImage setMainImage(Integer productId, Integer imageId) {
        log.info("메인 이미지 설정: productId={}, imageId={}", productId, imageId);

        // 기존 메인 이미지 해제
        List<ProductImage> allImages = getImagesByProductId(productId);
        for (ProductImage img : allImages) {
            if ("Y".equals(img.getIsMainImage())) {
                img.setIsMainImage("N");
                imageRepository.save(img);
                log.debug("기존 메인 이미지 해제: imageId={}", img.getImageId());
            }
        }

        // 새로운 메인 이미지 설정
        Optional<ProductImage> targetImage = imageRepository.findById(imageId);
        if (targetImage.isPresent()) {
            ProductImage image = targetImage.get();
            image.setIsMainImage("Y");
            ProductImage saved = imageRepository.save(image);
            log.info("새 메인 이미지 설정 완료: imageId={}", imageId);
            return saved;
        }

        throw new IllegalArgumentException("Image not found: " + imageId);
    }

    public void updateImageSequence(Integer imageId, Integer newSeq) {
        log.info("이미지 순서 변경: imageId={}, newSeq={}", imageId, newSeq);

        Optional<ProductImage> imageOpt = imageRepository.findById(imageId);
        if (imageOpt.isPresent()) {
            ProductImage image = imageOpt.get();
            image.setImageSeq(newSeq);
            imageRepository.save(image);
        }
    }

    public void deleteAllImagesByProductId(Integer productId) {
        log.info("상품의 모든 이미지 삭제: productId={}", productId);

        List<ProductImage> images = getImagesByProductId(productId);
        imageRepository.deleteAll(images);

        log.info("삭제된 이미지 수: {}", images.size());
    }

    // ========== API 응답용 메서드들 (URL 완성 포함) ==========

    public List<ProductImageDto> getProductImages(Integer productId) {
        log.debug("상품 이미지 DTO 조회: productId={}", productId);

        List<ProductImageDto> dtos = getImagesByProductId(productId).stream()
                .map(ProductImageDto::from)
                .collect(Collectors.toList());

        //  완전한 URL로 변환
        List<ProductImageDto> enrichedDtos = enrichWithCompleteUrls(dtos);

        log.debug("상품 {}의 이미지 DTO {}개 반환", productId, enrichedDtos.size());
        return enrichedDtos;
    }

    public ProductImageDto getMainImageDto(Integer productId) {
        log.debug("메인 이미지 DTO 조회: productId={}", productId);

        ProductImageDto dto = getMainImage(productId)
                .map(ProductImageDto::from)
                .orElse(null);

        //  완전한 URL로 변환
        ProductImageDto enrichedDto = enrichWithCompleteUrl(dto);

        log.debug("상품 {}의 메인 이미지 DTO: {}", productId, enrichedDto != null ? "존재" : "없음");
        return enrichedDto;
    }

    public Map<String, ProductImageDto> getMainImages(List<Integer> productIds) {
        log.debug("여러 상품의 메인 이미지 조회: 상품 수={}", productIds.size());

        Map<String, ProductImageDto> result = new HashMap<>();

        for (Integer productId : productIds) {
            try {
                ProductImageDto mainImage = getMainImageDto(productId);
                if (mainImage != null) {
                    result.put(productId.toString(), mainImage);
                }
            } catch (Exception e) {
                log.warn("상품 {}의 메인 이미지 조회 실패: {}", productId, e.getMessage());
            }
        }

        log.debug("메인 이미지 조회 결과: 요청 {}개, 성공 {}개", productIds.size(), result.size());
        return result;
    }

    public Map<String, List<ProductImageDto>> getProductImages(List<Integer> productIds) {
        log.debug("여러 상품의 모든 이미지 조회: 상품 수={}", productIds.size());

        Map<String, List<ProductImageDto>> result = new HashMap<>();

        for (Integer productId : productIds) {
            try {
                List<ProductImageDto> images = getProductImages(productId);
                result.put(productId.toString(), images);
            } catch (Exception e) {
                log.warn("상품 {}의 이미지 목록 조회 실패: {}", productId, e.getMessage());
                result.put(productId.toString(), List.of()); // 빈 리스트로 설정
            }
        }

        log.debug("이미지 목록 조회 결과: 요청 {}개 상품 처리 완료", productIds.size());
        return result;
    }

    public boolean hasImages(Integer productId) {
        boolean hasImages = !getImagesByProductId(productId).isEmpty();
        log.debug("상품 {}의 이미지 존재 여부: {}", productId, hasImages);
        return hasImages;
    }

    public boolean hasMainImage(Integer productId) {
        boolean hasMainImage = getMainImage(productId).isPresent();
        log.debug("상품 {}의 메인 이미지 존재 여부: {}", productId, hasMainImage);
        return hasMainImage;
    }

    // ========== 추가 유틸리티 메서드들 ==========

    /**
     * 상품의 이미지 개수 조회
     */
    public int getImageCount(Integer productId) {
        int count = getImagesByProductId(productId).size();
        log.debug("상품 {}의 이미지 개수: {}", productId, count);
        return count;
    }

    /**
     * 특정 순서의 이미지 조회
     */
    public Optional<ProductImageDto> getImageBySequence(Integer productId, Integer imageSeq) {
        log.debug("상품 {}의 {}번째 이미지 조회", productId, imageSeq);

        List<ProductImage> images = getImagesByProductId(productId);
        Optional<ProductImage> targetImage = images.stream()
                .filter(img -> imageSeq.equals(img.getImageSeq()))
                .findFirst();

        if (targetImage.isPresent()) {
            ProductImageDto dto = ProductImageDto.from(targetImage.get());
            return Optional.of(enrichWithCompleteUrl(dto));
        }

        return Optional.empty();
    }

    /**
     * 이미지 순서 재정렬
     */
    @Transactional
    public void reorderImages(Integer productId, List<Integer> imageIds) {
        log.info("상품 {}의 이미지 순서 재정렬 시작", productId);

        for (int i = 0; i < imageIds.size(); i++) {
            Integer imageId = imageIds.get(i);
            updateImageSequence(imageId, i + 1);
        }

        log.info("상품 {}의 이미지 순서 재정렬 완료: {}개 이미지", productId, imageIds.size());
    }

    /**
     * 현재 설정 정보 조회 (디버깅용)
     */
    public Map<String, String> getCurrentConfig() {
        Map<String, String> config = new HashMap<>();
        config.put("baseUrl", baseUrl);
        config.put("serverDomain", serverDomain);
        config.put("serverPort", serverPort);
        config.put("serverProtocol", serverProtocol);

        // 샘플 URL 생성
        String sampleUrl = buildCompleteUrl("/api/images/products/sample.jpg");
        config.put("sampleUrl", sampleUrl);

        return config;
    }
}