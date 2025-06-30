package org.kosa.productservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kosa.productservice.client.ImageServiceClient;
import org.kosa.productservice.dto.ApiResponse;
import org.kosa.productservice.dto.ProductDTO;
import org.kosa.productservice.dto.ProductImageDto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductImageIntegrationService {

    private final ImageServiceClient imageServiceClient;

    /**
     * 상품에 이미지 정보 추가
     */
    public void attachImagesToProduct(ProductDTO product) {
        try {
            String productIdStr = product.getProductId().toString();

            // 상품 이미지 목록 조회
            ApiResponse<List<ProductImageDto>> imagesResponse = imageServiceClient.getProductImages(productIdStr);
            if (imagesResponse.isSuccess() && imagesResponse.getData() != null) {
                // ProductImageDto 리스트를 ProductDto에 저장 (별도 필드)
                product.setProductImages(imagesResponse.getData());

                // 이미지 URL 리스트 생성 (기존 images 필드용)
                List<String> imageUrls = imagesResponse.getData().stream()
                        .map(ProductImageDto::getImageUrl)
                        .collect(Collectors.toList());
                product.setImages(imageUrls);
            } else {
                product.setProductImages(new ArrayList<>());
                product.setImages(new ArrayList<>());
            }

            // 대표 이미지 조회
            ApiResponse<ProductImageDto> mainImageResponse = imageServiceClient.getMainImage(productIdStr);
            if (mainImageResponse.isSuccess() && mainImageResponse.getData() != null) {
                ProductImageDto mainImageDto = mainImageResponse.getData();
                // String 타입의 mainImage 필드에 URL만 저장
                product.setMainImage(mainImageDto.getImageUrl());
                product.setImage(mainImageDto.getImageUrl()); // image 필드도 동일하게 설정
            }

        } catch (Exception e) {
            log.warn("상품 {} 이미지 연동 실패: {}", product.getProductId(), e.getMessage());
            product.setProductImages(new ArrayList<>());
            product.setImages(new ArrayList<>());
        }
    }

    /**
     * 여러 상품에 대표 이미지만 추가 (목록 조회용)
     */
    public void attachMainImagesToProducts(List<ProductDTO> products) {
        try {
            if (products == null || products.isEmpty()) {
                return;
            }

            List<String> productIds = products.stream()
                    .map(product -> product.getProductId().toString())
                    .collect(Collectors.toList());

            ApiResponse<Map<String, ProductImageDto>> mainImagesResponse = imageServiceClient.getMainImages(productIds);

            if (mainImagesResponse.isSuccess() && mainImagesResponse.getData() != null) {
                Map<String, ProductImageDto> mainImages = mainImagesResponse.getData();

                products.forEach(product -> {
                    ProductImageDto mainImage = mainImages.get(product.getProductId().toString());
                    if (mainImage != null) {
                        // String 타입으로 URL만 저장
                        product.setMainImage(mainImage.getImageUrl());
                        product.setImage(mainImage.getImageUrl());
                    }
                });
            }

        } catch (Exception e) {
            log.warn("여러 상품 대표 이미지 연동 실패: {}", e.getMessage());
        }
    }

    /**
     * 여러 상품에 모든 이미지 추가
     */
    public void attachAllImagesToProducts(List<ProductDTO> products) {
        try {
            if (products == null || products.isEmpty()) {
                return;
            }

            List<String> productIds = products.stream()
                    .map(product -> product.getProductId().toString())
                    .collect(Collectors.toList());

            // 모든 이미지 조회
            ApiResponse<Map<String, List<ProductImageDto>>> allImagesResponse = imageServiceClient.getProductImages(productIds);

            // 대표 이미지 조회
            ApiResponse<Map<String, ProductImageDto>> mainImagesResponse = imageServiceClient.getMainImages(productIds);

            if (allImagesResponse.isSuccess() && allImagesResponse.getData() != null) {
                Map<String, List<ProductImageDto>> allImages = allImagesResponse.getData();
                Map<String, ProductImageDto> mainImages = mainImagesResponse.isSuccess() ?
                        mainImagesResponse.getData() : new HashMap<>();

                products.forEach(product -> {
                    String productIdStr = product.getProductId().toString();

                    // 모든 이미지 설정
                    List<ProductImageDto> productImageDtos = allImages.getOrDefault(productIdStr, new ArrayList<>());
                    product.setProductImages(productImageDtos);

                    // 이미지 URL 리스트 설정 (String 타입 리스트로 변환)
                    List<String> imageUrls = productImageDtos.stream()
                            .map(ProductImageDto::getImageUrl)
                            .collect(Collectors.toList());
                    product.setImages(imageUrls);

                    // 대표 이미지 설정 (String 타입으로)
                    ProductImageDto mainImage = mainImages.get(productIdStr);
                    if (mainImage != null) {
                        product.setMainImage(mainImage.getImageUrl());
                        product.setImage(mainImage.getImageUrl());
                    }
                });
            }

        } catch (Exception e) {
            log.warn("여러 상품 이미지 연동 실패: {}", e.getMessage());
        }
    }

    /**
     * 단일 상품에 대표 이미지만 추가 (성능 최적화용)
     */
    public void attachMainImageToProduct(ProductDTO product) {
        try {
            if (product == null) {
                return;
            }

            String productIdStr = product.getProductId().toString();

            // 대표 이미지 조회
            ApiResponse<ProductImageDto> mainImageResponse = imageServiceClient.getMainImage(productIdStr);
            if (mainImageResponse.isSuccess() && mainImageResponse.getData() != null) {
                ProductImageDto mainImageDto = mainImageResponse.getData();
                product.setMainImage(mainImageDto.getImageUrl());
                product.setImage(mainImageDto.getImageUrl());
            }

        } catch (Exception e) {
            log.warn("상품 {} 대표 이미지 연동 실패: {}", product.getProductId(), e.getMessage());
        }
    }

    /**
     * 이미지 서비스 연결 상태 확인
     */
    public boolean isImageServiceAvailable() {
        try {
            // 더미 상품 ID로 연결 테스트
            ApiResponse<ProductImageDto> testResponse = imageServiceClient.getMainImage("TEST");
            return testResponse != null; // 응답만 받으면 연결됨으로 판단
        } catch (Exception e) {
            log.warn("이미지 서비스 연결 실패: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 대량 이미지 처리용 배치 메서드
     */
    public void batchProcessImages(List<ProductDTO> products, boolean includeAllImages) {
        if (products == null || products.isEmpty()) {
            return;
        }

        log.info("배치 이미지 처리 시작: {} 상품, 전체이미지포함: {}", products.size(), includeAllImages);

        try {
            if (includeAllImages) {
                attachAllImagesToProducts(products);
            } else {
                attachMainImagesToProducts(products);
            }
            log.info("배치 이미지 처리 완료: {} 상품", products.size());
        } catch (Exception e) {
            log.error("배치 이미지 처리 실패: {} 상품", products.size(), e);
            // 실패 시 기본 이미지 설정
            products.forEach(this::setDefaultImage);
        }
    }

    /**
     * 기본 이미지 설정
     */
    private void setDefaultImage(ProductDTO product) {
        String defaultImageUrl = getDefaultImageUrl();
        product.setMainImage(defaultImageUrl);
        product.setImage(defaultImageUrl);
        product.setImages(List.of(defaultImageUrl));
        product.setProductImages(new ArrayList<>());
    }

    /**
     * 기본 이미지 URL 생성
     */
    private String getDefaultImageUrl() {
        return "data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMzAwIiBoZWlnaHQ9IjIwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iMTAwJSIgaGVpZ2h0PSIxMDAlIiBmaWxsPSIjZGRkIi8+PHRleHQgeD0iNTAlIiB5PSI1MCUiIGZvbnQtZmFtaWx5PSJBcmlhbCIgZm9udC1zaXplPSIxNCIgZmlsbD0iIzk5OSIgdGV4dC1hbmNob3I9Im1pZGRsZSIgZHk9Ii4zZW0iPk5vIEltYWdlPC90ZXh0Pjwvc3ZnPg==";
    }

    /**
     * 이미지 URL 검증
     */
    public boolean isValidImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            return false;
        }

        return imageUrl.startsWith("http://") ||
                imageUrl.startsWith("https://") ||
                imageUrl.startsWith("data:image/");
    }

    /**
     * 이미지 메타데이터 로깅
     */
    public void logImageMetadata(ProductDTO product) {
        if (log.isDebugEnabled()) {
            log.debug("상품 {} 이미지 메타데이터:", product.getProductId());
            log.debug("- 메인 이미지: {}", product.getMainImage());
            log.debug("- 이미지 개수: {}", product.getImages() != null ? product.getImages().size() : 0);
            log.debug("- ProductImageDto 개수: {}", product.getProductImages() != null ? product.getProductImages().size() : 0);
        }
    }
}