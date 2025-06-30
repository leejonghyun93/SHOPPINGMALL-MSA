package org.kosa.categoryservice.dto;

import lombok.*;
import org.kosa.categoryservice.entity.Category;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDto {
    private Integer categoryId;
    private Integer parentCategoryId;
    private String name;
    private Integer categoryLevel;
    private Integer categoryDisplayOrder;
    private String categoryUseYn;
    private List<CategoryDto> subCategories;
    private String categoryIcon;
    private String iconUrl;
    private Category.IconType iconType;

    // 기존 호환성을 위한 필드들
    private String icon;  // 프론트엔드에서 사용하는 아이콘 URL
    private String path;  // 필요시 사용
}