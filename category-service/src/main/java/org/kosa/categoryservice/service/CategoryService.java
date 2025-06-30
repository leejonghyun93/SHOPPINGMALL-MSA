package org.kosa.categoryservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kosa.categoryservice.entity.Category;
import org.kosa.categoryservice.dto.CategoryDto;
import org.kosa.categoryservice.mapper.CategoryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.ArrayList;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Value("${app.icon.base-url:/icons}")
    private String iconBaseUrl;

    @Value("${server.port:8085}")
    private String serverPort;

    /**
     * 최상위 카테고리 목록 조회 - Redis 캐시 적용
     */
    @Cacheable(value = "categories", key = "'main'")
    public List<CategoryDto> getMainCategories() {
        log.debug("메인 카테고리 조회 시작");

        List<CategoryDto> result = categoryRepository
                .findByCategoryLevelAndCategoryUseYnOrderByCategoryDisplayOrder(1, "Y")
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        log.debug("메인 카테고리 {}개 조회 완료", result.size());

        return result;
    }

    /**
     * 특정 카테고리의 하위 카테고리 조회 - Redis 캐시 적용
     */
    @Cacheable(value = "categories", key = "'sub:' + #parentCategoryId")
    public List<CategoryDto> getSubCategories(Integer parentCategoryId) {

        List<Category> subCategories = categoryRepository
                .findByParentCategory_CategoryIdAndCategoryUseYnOrderByCategoryDisplayOrder(parentCategoryId, "Y");

        List<CategoryDto> result = subCategories.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        return result;
    }

    /**
     * 특정 카테고리의 모든 하위 카테고리 ID 목록 조회 (재귀적) - Redis 캐시 적용
     */
    @Cacheable(value = "categories", key = "'children:' + #parentCategoryId")
    public List<Integer> getAllChildrenIds(Integer parentCategoryId) {

        List<Integer> allChildrenIds = new ArrayList<>();
        collectAllChildrenIds(parentCategoryId, allChildrenIds);

        return allChildrenIds;
    }

    /**
     * 재귀적으로 하위 카테고리 ID 수집
     */
    private void collectAllChildrenIds(Integer parentCategoryId, List<Integer> result) {
        List<Category> directChildren = categoryRepository
                .findByParentCategory_CategoryIdAndCategoryUseYnOrderByCategoryDisplayOrder(parentCategoryId, "Y");

        for (Category child : directChildren) {
            result.add(child.getCategoryId());
            collectAllChildrenIds(child.getCategoryId(), result);
        }
    }

    /**
     * 카테고리 존재 여부 확인 (캐시 없음 - 빠른 조회)
     */
    public boolean existsCategory(Integer categoryId) {
        return categoryRepository.existsById(categoryId);
    }

    /**
     * 카테고리 계층 구조로 조회 - Redis 캐시 적용
     */
    @Cacheable(value = "categories", key = "'hierarchy'")
    public List<CategoryDto> getCategoriesWithHierarchy() {

        List<Category> mainCategories = categoryRepository
                .findByParentCategoryIsNullAndCategoryUseYnOrderByCategoryDisplayOrder("Y");

        List<CategoryDto> result = mainCategories.stream()
                .map(this::convertToDtoWithSubCategories)
                .collect(Collectors.toList());

        return result;
    }

    /**
     * 특정 카테고리 정보 조회 - Redis 캐시 적용
     */
    @Cacheable(value = "categories", key = "'detail:' + #categoryId")
    public CategoryDto getCategory(Integer categoryId) {

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("카테고리를 찾을 수 없습니다: " + categoryId));

        CategoryDto result = convertToDto(category);
        return result;
    }

    /**
     * 카테고리 캐시 무효화 (관리용)
     */
    @CacheEvict(value = "categories", allEntries = true)
    @Transactional
    public void evictAllCategoryCache() {
        log.info("모든 카테고리 캐시 무효화");
    }

    @CacheEvict(value = "categories", key = "'detail:' + #categoryId")
    @Transactional
    public void evictCategoryCache(Integer categoryId) {
        log.info("카테고리 {} 캐시 무효화", categoryId);
    }

    /**
     * Entity를 DTO로 변환
     */
    private CategoryDto convertToDto(Category category) {
        CategoryDto dto = new CategoryDto();
        dto.setCategoryId(category.getCategoryId());
        dto.setName(category.getName());
        dto.setCategoryLevel(category.getCategoryLevel());
        dto.setCategoryDisplayOrder(category.getCategoryDisplayOrder());
        dto.setCategoryUseYn(category.getCategoryUseYn());

        // DB의 categoryIcon 필드 활용
        dto.setCategoryIcon(category.getCategoryIcon());

        // 아이콘 URL 생성 로직
        String iconUrl = buildIconUrl(category.getCategoryIcon());
        dto.setIcon(iconUrl);

        // iconUrl 필드도 설정 (프론트엔드 호환성)
        dto.setIconUrl(iconUrl);

        // 부모 카테고리 ID 설정
        if (category.getParentCategory() != null) {
            dto.setParentCategoryId(category.getParentCategory().getCategoryId());
        }

        return dto;
    }

    /**
     * 하위 카테고리까지 포함해서 DTO로 변환
     */
    private CategoryDto convertToDtoWithSubCategories(Category category) {
        List<Category> subCategories = categoryRepository
                .findByParentCategory_CategoryIdAndCategoryUseYnOrderByCategoryDisplayOrder(category.getCategoryId(), "Y");

        List<CategoryDto> subCategoryDtos = subCategories.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        CategoryDto dto = convertToDto(category);
        dto.setSubCategories(subCategoryDtos);
        return dto;
    }

    /**
     * 아이콘 URL 생성 메서드
     */
    private String buildIconUrl(String categoryIcon) {
        if (categoryIcon == null || categoryIcon.trim().isEmpty()) {
            log.debug("카테고리 아이콘이 null이거나 비어있음");
            return null;
        }

        // 아이콘명 → 파일명 매핑
        Map<String, String> iconFileMap = Map.of(
                "vegetables", "vegetables.svg",
                "canned", "canned-food.svg",
                "meal", "meal-box.svg",
                "bread", "bread.svg",
                "milk", "milk.svg",
                "medicine", "medicine.svg",
                "cooking", "cooking.svg",
                "tissue", "tissue.svg",
                "baby", "baby-bottle.svg"
        );

        // 매핑된 파일명이 있으면 사용, 없으면 원본명에 .svg 추가
        String iconFile = iconFileMap.getOrDefault(categoryIcon, categoryIcon + ".svg");

        // 🔥 프론트엔드에서 접근 가능한 절대 URL 생성
        // 옵션 1: 백엔드 서버를 통한 정적 파일 서빙
        String backendIconUrl = "http://localhost:" + serverPort + iconBaseUrl + "/" + iconFile;

        // 옵션 2: 프론트엔드 public 폴더의 파일 (권장)
        String frontendIconUrl = "/icons/" + iconFile;

        log.debug("카테고리 아이콘: {} -> 파일: {} -> URL: {}", categoryIcon, iconFile, frontendIconUrl);

        // 프론트엔드 경로 반환 (public/icons/ 폴더에서 직접 서빙)
        return frontendIconUrl;
    }
}