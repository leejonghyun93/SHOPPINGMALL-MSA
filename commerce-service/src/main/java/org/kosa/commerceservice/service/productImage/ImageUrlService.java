package org.kosa.commerceservice.service.productImage;

import org.kosa.commerceservice.dto.productImage.ProductImageDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Component
public class ImageUrlService {

    @Value("${image.base-url}")
    private String baseUrl;

    @Value("${server.domain:localhost}")
    private String serverDomain;

    @Value("${server.port:8080}")
    private String serverPort;

    @Value("${server.protocol:http}")
    private String serverProtocol;

    /**
     * 상대 경로를 완전한 URL로 변환
     */
    public String buildCompleteUrl(String relativePath) {
        if (relativePath == null || relativePath.trim().isEmpty()) {
            return null;
        }

        // 이미 완전한 URL인 경우
        if (relativePath.startsWith("http://") || relativePath.startsWith("https://")) {
            return relativePath;
        }

        // baseUrl이 설정된 경우 사용
        if (baseUrl != null && !baseUrl.trim().isEmpty()) {
            return baseUrl + (relativePath.startsWith("/") ? relativePath.substring(1) : relativePath);
        }

        // 동적으로 URL 생성
        String domain = "localhost".equals(serverDomain) ? "localhost" : serverDomain;
        String port = "80".equals(serverPort) || "443".equals(serverPort) ? "" : ":" + serverPort;

        return String.format("%s://%s%s%s",
                serverProtocol,
                domain,
                port,
                relativePath.startsWith("/") ? relativePath : "/" + relativePath
        );
    }

    /**
     * ProductImageDto에 완전한 URL 설정
     */
    public ProductImageDto enrichWithCompleteUrl(ProductImageDto dto) {
        if (dto != null && dto.getImageUrl() != null) {
            dto.setImageUrl(buildCompleteUrl(dto.getImageUrl()));
        }
        return dto;
    }

    /**
     * ProductImageDto 리스트에 완전한 URL 설정
     */
    public List<ProductImageDto> enrichWithCompleteUrls(List<ProductImageDto> dtos) {
        return dtos.stream()
                .map(this::enrichWithCompleteUrl)
                .collect(Collectors.toList());
    }
}
