package org.kosa.productcatalogservice.productcatalog.dto;

import lombok.*;
import org.kosa.productcatalogservice.productcatalog.entity.Category;

import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
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
    private String icon;
    private String path;
}