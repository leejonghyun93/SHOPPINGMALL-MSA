package org.kosa.commerceservice.service.category;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.kosa.commerceservice.dto.category.CategoryDto;
import org.kosa.commerceservice.entity.category.Category;
import org.kosa.commerceservice.repository.category.CategoryRepository;
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

    @Value("${server.port:8080}")
    private String serverPort;

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

    @Cacheable(value = "categories", key = "'sub:' + #parentCategoryId")
    public List<CategoryDto> getSubCategories(Integer parentCategoryId) {
        List<Category> subCategories = categoryRepository
                .findByParentCategory_CategoryIdAndCategoryUseYnOrderByCategoryDisplayOrder(parentCategoryId, "Y");

        List<CategoryDto> result = subCategories.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        return result;
    }

    @Cacheable(value = "categories", key = "'children:' + #parentCategoryId")
    public List<Integer> getAllChildrenIds(Integer parentCategoryId) {
        List<Integer> allChildrenIds = new ArrayList<>();
        collectAllChildrenIds(parentCategoryId, allChildrenIds);
        return allChildrenIds;
    }

    private void collectAllChildrenIds(Integer parentCategoryId, List<Integer> result) {
        List<Category> directChildren = categoryRepository
                .findByParentCategory_CategoryIdAndCategoryUseYnOrderByCategoryDisplayOrder(parentCategoryId, "Y");

        for (Category child : directChildren) {
            result.add(child.getCategoryId());
            collectAllChildrenIds(child.getCategoryId(), result);
        }
    }

    public boolean existsCategory(Integer categoryId) {
        return categoryRepository.existsById(categoryId);
    }

    @Cacheable(value = "categories", key = "'hierarchy'")
    public List<CategoryDto> getCategoriesWithHierarchy() {
        List<Category> mainCategories = categoryRepository
                .findByParentCategoryIsNullAndCategoryUseYnOrderByCategoryDisplayOrder("Y");

        List<CategoryDto> result = mainCategories.stream()
                .map(this::convertToDtoWithSubCategories)
                .collect(Collectors.toList());

        return result;
    }

    @Cacheable(value = "categories", key = "'detail:' + #categoryId")
    public CategoryDto getCategory(Integer categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("카테고리를 찾을 수 없습니다: " + categoryId));

        CategoryDto result = convertToDto(category);
        return result;
    }

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

    private CategoryDto convertToDto(Category category) {
        CategoryDto dto = new CategoryDto();
        dto.setCategoryId(category.getCategoryId());
        dto.setName(category.getName());
        dto.setCategoryLevel(category.getCategoryLevel());
        dto.setCategoryDisplayOrder(category.getCategoryDisplayOrder());
        dto.setCategoryUseYn(category.getCategoryUseYn());
        dto.setCategoryIcon(category.getCategoryIcon());

        String iconUrl = buildIconUrl(category.getCategoryIcon());
        dto.setIcon(iconUrl);
        dto.setIconUrl(iconUrl);

        if (category.getParentCategory() != null) {
            dto.setParentCategoryId(category.getParentCategory().getCategoryId());
        }

        return dto;
    }

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

    private String buildIconUrl(String categoryIcon) {
        if (categoryIcon == null || categoryIcon.trim().isEmpty()) {
            log.debug("카테고리 아이콘이 null이거나 비어있음");
            return null;
        }

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

        String iconFile = iconFileMap.getOrDefault(categoryIcon, categoryIcon + ".svg");
        String frontendIconUrl = "/icons/" + iconFile;

        log.debug("카테고리 아이콘: {} -> 파일: {} -> URL: {}", categoryIcon, iconFile, frontendIconUrl);
        return frontendIconUrl;
    }
}