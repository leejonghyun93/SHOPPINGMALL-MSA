package org.kosa.commerceservice.controller.category;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.kosa.commerceservice.dto.category.CategoryDto;
import org.kosa.commerceservice.service.category.CategoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Comparator;

@Tag(name = "카테고리 API", description = "상품 카테고리 조회 및 관리 API")
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Slf4j
public class CategoryApiController {

    private final CategoryService categoryService;

    @Operation(summary = "메인 카테고리 조회", description = "최상위 레벨의 메인 카테고리 목록을 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = CategoryDto.class)))
    })
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

    @Operation(summary = "하위 카테고리 조회", description = "특정 카테고리의 하위 카테고리 목록을 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping("/{categoryId}/sub")
    public ResponseEntity<List<CategoryDto>> getSubCategories(
            @Parameter(description = "상위 카테고리 ID", required = true, example = "1")
            @PathVariable Integer categoryId) {
        log.debug("하위 카테고리 조회 요청: {}", categoryId);

        List<CategoryDto> subCategories = categoryService.getSubCategories(categoryId).stream()
                .filter(cat -> "Y".equals(cat.getCategoryUseYn()))
                .sorted(Comparator.comparingInt(CategoryDto::getCategoryDisplayOrder))
                .collect(Collectors.toList());

        log.debug("하위 카테고리 {}개 조회됨", subCategories.size());
        return ResponseEntity.ok(subCategories);
    }

    @Operation(summary = "하위 카테고리 ID 목록 조회", description = "특정 카테고리의 모든 하위 카테고리 ID 목록을 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping("/{categoryId}/children-ids")
    public ResponseEntity<List<Integer>> getChildrenIds(
            @Parameter(description = "카테고리 ID", required = true, example = "1")
            @PathVariable Integer categoryId) {
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

    @Operation(summary = "카테고리 존재 여부 확인", description = "특정 카테고리의 존재 여부를 확인합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = Boolean.class)))
    })
    @GetMapping("/{categoryId}/exists")
    public ResponseEntity<Boolean> existsCategory(
            @Parameter(description = "카테고리 ID", required = true, example = "1")
            @PathVariable Integer categoryId) {
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

    @Operation(summary = "계층형 카테고리 조회", description = "전체 카테고리를 계층 구조로 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping("/hierarchy")
    public ResponseEntity<List<CategoryDto>> getCategoriesWithHierarchy() {
        log.debug("계층형 카테고리 조회 요청");

        List<CategoryDto> categories = categoryService.getCategoriesWithHierarchy();

        log.debug("계층형 카테고리 {}개 조회됨", categories.size());
        return ResponseEntity.ok(categories);
    }

    @Operation(summary = "카테고리 상세 조회", description = "특정 카테고리의 상세 정보를 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = CategoryDto.class)))
    })
    @GetMapping("/{categoryId}")
    public ResponseEntity<CategoryDto> getCategory(
            @Parameter(description = "카테고리 ID", required = true, example = "1")
            @PathVariable Integer categoryId) {
        log.debug("카테고리 상세 조회 요청: {}", categoryId);

        CategoryDto category = categoryService.getCategory(categoryId);

        return ResponseEntity.ok(category);
    }
}