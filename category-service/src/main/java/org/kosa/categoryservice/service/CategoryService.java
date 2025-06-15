package org.kosa.categoryservice.service;

import lombok.RequiredArgsConstructor;
import org.kosa.categoryservice.dto.Category;
import org.kosa.categoryservice.dto.CategoryDto;
import org.kosa.categoryservice.mapper.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class CategoryService {

    private final CategoryRepository categoryRepository;

    /**
     * 최상위 카테고리 목록 조회
     */
    public List<CategoryDto> getMainCategories() {
        // 대분류만 조회하는 메서드로 변경
        return categoryRepository.findByCategoryLevelAndCategoryUseYnOrderByCategoryDisplayOrder(1, "Y")
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * 특정 카테고리의 하위 카테고리 조회
     */
    public List<CategoryDto> getSubCategories(String parentCategoryId) {
        List<Category> subCategories = categoryRepository
                .findByParentCategory_CategoryIdAndCategoryUseYnOrderByCategoryDisplayOrder(parentCategoryId, "Y");

        log.debug("하위 카테고리 {}개 조회 (부모: {})", subCategories.size(), parentCategoryId);

        return subCategories.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * 카테고리 계층 구조로 조회
     */
    public List<CategoryDto> getCategoriesWithHierarchy() {
        List<Category> mainCategories = categoryRepository
                .findByParentCategoryIsNullAndCategoryUseYnOrderByCategoryDisplayOrder("Y");

        return mainCategories.stream()
                .map(this::convertToDtoWithSubCategories)
                .collect(Collectors.toList());
    }

    /**
     * 특정 카테고리 정보 조회
     */
    public CategoryDto getCategory(String categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("카테고리를 찾을 수 없습니다: " + categoryId));

        return convertToDto(category);
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

    /**
     * 카테고리별 경로 매핑
     */
    private String getPathForCategory(String categoryId) {
        Map<String, String> pathMap = Map.of(
                "FRESH", "1",
                "PROCESSED", "2",
                "MEAL_KIT", "3",
                "BAKERY", "4",
                "DAIRY", "5",
                "HEALTH", "6",
                "KITCHEN", "7",
                "LIVING", "8",
                "BABY", "9"
        );
        return pathMap.getOrDefault(categoryId, "");
    }
}