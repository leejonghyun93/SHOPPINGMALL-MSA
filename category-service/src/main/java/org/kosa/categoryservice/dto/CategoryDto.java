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
    private String icon;
    private String path;
    private String iconUrl;
    private String categoryIcon;
}