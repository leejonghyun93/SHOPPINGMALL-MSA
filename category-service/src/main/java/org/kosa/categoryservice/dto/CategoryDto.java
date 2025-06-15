package org.kosa.categoryservice.dto;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDto {
    private String categoryId;
    private String parentCategoryId;
    private String name;
    private Integer categoryLevel;
    private Integer categoryDisplayOrder;
    private String categoryUseYn;
    private List<CategoryDto> subCategories;
    private String icon; // 프론트엔드에서 사용할 아이콘 정보
    private String path; // 프론트엔드에서 사용할 경로 정보
}