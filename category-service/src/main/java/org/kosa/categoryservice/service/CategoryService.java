package org.kosa.categoryservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kosa.categoryservice.dto.Category;
import org.kosa.categoryservice.dto.CategoryDto;
import org.kosa.categoryservice.mapper.CategoryRepository;
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

    /**
     * 🔥 최상위 카테고리 목록 조회 - Redis 캐시 적용
     */
    @Cacheable(value = "categories", key = "'main'")
    public List<CategoryDto> getMainCategories() {
        log.info("🔍 DB에서 메인 카테고리 조회");

        List<CategoryDto> result = categoryRepository
                .findByCategoryLevelAndCategoryUseYnOrderByCategoryDisplayOrder(1, "Y")
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        log.info("✅ 메인 카테고리 조회 완료 (캐시 저장): {}개", result.size());
        return result;
    }

    /**
     * 🔥 특정 카테고리의 하위 카테고리 조회 - Redis 캐시 적용
     */
    @Cacheable(value = "categories", key = "'sub:' + #parentCategoryId")
    public List<CategoryDto> getSubCategories(String parentCategoryId) {
        log.info("🔍 DB에서 하위 카테고리 조회: {}", parentCategoryId);

        List<Category> subCategories = categoryRepository
                .findByParentCategory_CategoryIdAndCategoryUseYnOrderByCategoryDisplayOrder(parentCategoryId, "Y");

        List<CategoryDto> result = subCategories.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        log.info("✅ 하위 카테고리 조회 완료 (캐시 저장): {}개", result.size());
        return result;
    }

    /**
     * 🔥 특정 카테고리의 모든 하위 카테고리 ID 목록 조회 (재귀적) - Redis 캐시 적용
     */
    @Cacheable(value = "categories", key = "'children:' + #parentCategoryId")
    public List<String> getAllChildrenIds(String parentCategoryId) {
        log.info("🔍 DB에서 모든 하위 카테고리 ID 조회: {}", parentCategoryId);

        List<String> allChildrenIds = new ArrayList<>();
        collectAllChildrenIds(parentCategoryId, allChildrenIds);

        log.info("✅ 모든 하위 카테고리 ID 조회 완료 (캐시 저장): {}", allChildrenIds);
        return allChildrenIds;
    }

    /**
     * 재귀적으로 하위 카테고리 ID 수집
     */
    private void collectAllChildrenIds(String parentCategoryId, List<String> result) {
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
    public boolean existsCategory(String categoryId) {
        return categoryRepository.existsById(categoryId);
    }

    /**
     * 🔥 카테고리 계층 구조로 조회 - Redis 캐시 적용
     */
    @Cacheable(value = "categories", key = "'hierarchy'")
    public List<CategoryDto> getCategoriesWithHierarchy() {
        log.info("🔍 DB에서 계층 구조 카테고리 조회");

        List<Category> mainCategories = categoryRepository
                .findByParentCategoryIsNullAndCategoryUseYnOrderByCategoryDisplayOrder("Y");

        List<CategoryDto> result = mainCategories.stream()
                .map(this::convertToDtoWithSubCategories)
                .collect(Collectors.toList());

        log.info("✅ 계층 구조 카테고리 조회 완료 (캐시 저장): {}개", result.size());
        return result;
    }

    /**
     * 🔥 특정 카테고리 정보 조회 - Redis 캐시 적용
     */
    @Cacheable(value = "categories", key = "'detail:' + #categoryId")
    public CategoryDto getCategory(String categoryId) {
        log.info("🔍 DB에서 카테고리 상세 조회: {}", categoryId);

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("카테고리를 찾을 수 없습니다: " + categoryId));

        CategoryDto result = convertToDto(category);
        log.info("✅ 카테고리 상세 조회 완료 (캐시 저장): {}", categoryId);
        return result;
    }

    /**
     * 🔥 카테고리 캐시 무효화 (관리용)
     */
    @CacheEvict(value = "categories", allEntries = true)
    @Transactional
    public void evictAllCategoryCache() {
        log.info("🗑️ 모든 카테고리 캐시 무효화");
    }

    @CacheEvict(value = "categories", key = "'detail:' + #categoryId")
    @Transactional
    public void evictCategoryCache(String categoryId) {
        log.info("🗑️ 특정 카테고리 캐시 무효화: {}", categoryId);
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

        // 아이콘 매핑 추가
        dto.setIcon(getIconForCategory(category.getCategoryId()));

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
     * 카테고리별 아이콘 매핑 (실제로는 DB에 저장하거나 설정 파일로 관리)
     */
    private String getIconForCategory(String categoryId) {
        Map<String, String> iconMap = Map.of(
                "1", "🥬", // 신선식품
                "2", "🥫", // 가공식품
                "3", "🍱", // 간편식/밀키트
                "4", "🍞", // 베이커리
                "5", "🥛", // 유제품/음료
                "6", "💊", // 건강식품
                "7", "🍳", // 주방용품
                "8", "🧻", // 생활용품
                "9", "🍼"  // 유아동
        );
        return iconMap.getOrDefault(categoryId, "📦");
    }
}