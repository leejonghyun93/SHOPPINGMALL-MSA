package org.kosa.commerceservice.controller.category;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.kosa.commerceservice.dto.category.CategoryDto;
import org.kosa.commerceservice.service.category.CategoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Comparator;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Slf4j
public class CategoryApiController {

    private final CategoryService categoryService;

    @GetMapping("/main")
    public ResponseEntity<List<CategoryDto>> getMainCategories() {
        log.debug("메인 카테고리 목록 조회 요청");

        List<CategoryDto> categories = categoryService.getMainCategories().stream()
                .filter(cat -> "Y".equals(cat.getCategoryUseYn()))
                .filter(cat -> cat.getCategoryLevel() == 1)
                .sorted(Comparator.comparingInt(CategoryDto::getCategoryDisplayOrder))
                .collect(Collectors.toList());

        log.debug("메인 카테고리 {}개 조회됨", categories.size());
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/{categoryId}/sub")
    public ResponseEntity<List<CategoryDto>> getSubCategories(@PathVariable Integer categoryId) {
        log.debug("하위 카테고리 조회 요청: {}", categoryId);

        List<CategoryDto> subCategories = categoryService.getSubCategories(categoryId).stream()
                .filter(cat -> "Y".equals(cat.getCategoryUseYn()))
                .sorted(Comparator.comparingInt(CategoryDto::getCategoryDisplayOrder))
                .collect(Collectors.toList());

        log.debug("하위 카테고리 {}개 조회됨", subCategories.size());
        return ResponseEntity.ok(subCategories);
    }

    @GetMapping("/{categoryId}/children-ids")
    public ResponseEntity<List<Integer>> getChildrenIds(@PathVariable Integer categoryId) {
        try {
            log.debug("카테고리 {} 하위 ID 목록 조회 요청", categoryId);

            List<Integer> childrenIds = categoryService.getAllChildrenIds(categoryId);

            log.debug("카테고리 {} 하위 ID 목록: {}", categoryId, childrenIds);

            return ResponseEntity.ok(childrenIds);

        } catch (Exception e) {
            log.error("하위 카테고리 ID 조회 실패: {}", e.getMessage(), e);
            return ResponseEntity.ok(Collections.emptyList());
        }
    }

    @GetMapping("/{categoryId}/exists")
    public ResponseEntity<Boolean> existsCategory(@PathVariable Integer categoryId) {
        try {
            log.debug("카테고리 {} 존재 여부 확인 요청", categoryId);

            boolean exists = categoryService.existsCategory(categoryId);

            log.debug("카테고리 {} 존재 여부: {}", categoryId, exists);

            return ResponseEntity.ok(exists);

        } catch (Exception e) {
            log.error("카테고리 존재 여부 확인 실패: {}", e.getMessage(), e);
            return ResponseEntity.ok(false);
        }
    }

    @GetMapping("/hierarchy")
    public ResponseEntity<List<CategoryDto>> getCategoriesWithHierarchy() {
        log.debug("계층형 카테고리 조회 요청");

        List<CategoryDto> categories = categoryService.getCategoriesWithHierarchy();

        log.debug("계층형 카테고리 {}개 조회됨", categories.size());
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/{categoryId}")
    public ResponseEntity<CategoryDto> getCategory(@PathVariable Integer categoryId) {
        log.debug("카테고리 상세 조회 요청: {}", categoryId);

        CategoryDto category = categoryService.getCategory(categoryId);

        return ResponseEntity.ok(category);
    }
}