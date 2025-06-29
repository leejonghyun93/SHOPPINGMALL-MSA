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
            // 상품 이미지 목록 조회
            ApiResponse<List<ProductImageDto>> imagesResponse = imageServiceClient.getProductImages(product.getProductId());
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
            ApiResponse<ProductImageDto> mainImageResponse = imageServiceClient.getMainImage(product.getProductId());
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
                    .map(ProductDTO::getProductId)
                    .collect(Collectors.toList());

            ApiResponse<Map<String, ProductImageDto>> mainImagesResponse = imageServiceClient.getMainImages(productIds);

            if (mainImagesResponse.isSuccess() && mainImagesResponse.getData() != null) {
                Map<String, ProductImageDto> mainImages = mainImagesResponse.getData();

                products.forEach(product -> {
                    ProductImageDto mainImage = mainImages.get(product.getProductId());
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
                    .map(ProductDTO::getProductId)
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
                    String productId = product.getProductId();

                    // 모든 이미지 설정
                    List<ProductImageDto> productImageDtos = allImages.getOrDefault(productId, new ArrayList<>());
                    product.setProductImages(productImageDtos);

                    // 이미지 URL 리스트 설정 (String 타입 리스트로 변환)
                    List<String> imageUrls = productImageDtos.stream()
                            .map(ProductImageDto::getImageUrl)
                            .collect(Collectors.toList());
                    product.setImages(imageUrls);

                    // 대표 이미지 설정 (String 타입으로)
                    ProductImageDto mainImage = mainImages.get(productId);
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

            // 대표 이미지 조회
            ApiResponse<ProductImageDto> mainImageResponse = imageServiceClient.getMainImage(product.getProductId());
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
}