package org.kosa.productservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class CategoryServiceClient {

    private final RestTemplate restTemplate;
    private final String categoryServiceUrl;

    public CategoryServiceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.categoryServiceUrl = "http://localhost:8080"; // 카테고리 서비스 URL
    }

    /**
     * 🔥 메인 카테고리의 하위 카테고리 ID 목록 조회
     */
    public List<String> getChildrenCategoryIds(String parentCategoryId) {
        try {
            String url = categoryServiceUrl + "/api/categories/" + parentCategoryId + "/children-ids";
            log.info("🔍 카테고리 서비스 호출: {}", url);

            ResponseEntity<List<String>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<String>>() {}
            );

            List<String> childrenIds = response.getBody();
            log.info("✅ 하위 카테고리 ID 목록: {}", childrenIds);
            return childrenIds != null ? childrenIds : new ArrayList<>();
        } catch (Exception e) {
            log.error("❌ 카테고리 서비스 호출 실패:", e);
            return new ArrayList<>();
        }
    }

    /**
     * 카테고리 존재 여부 확인
     */
    public boolean categoryExists(String categoryId) {
        try {
            String url = categoryServiceUrl + "/api/categories/" + categoryId + "/exists";
            ResponseEntity<Boolean> response = restTemplate.getForEntity(url, Boolean.class);
            return response.getBody() != null ? response.getBody() : false;
        } catch (Exception e) {
            log.error("카테고리 존재 여부 확인 실패:", e);
            return false;
        }
    }
}