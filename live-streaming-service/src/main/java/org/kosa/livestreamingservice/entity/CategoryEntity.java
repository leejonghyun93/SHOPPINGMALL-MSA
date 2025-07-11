package org.kosa.livestreamingservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_category")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CATEGORY_ID")
    private Integer categoryId;

    @Column(name = "parent_category_id")
    private Integer parentCategoryId;

    @Column(name = "name")
    private String name;

    @Column(name = "category_level")
    @Builder.Default
    private Integer categoryLevel = 1;

    @Column(name = "category_display_order")
    private Integer categoryDisplayOrder;

    @Column(name = "category_use_yn", columnDefinition = "CHAR(1)")
    @Builder.Default
    private String categoryUseYn = "Y";

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @Column(name = "updated_date")
    private LocalDateTime updatedDate;

    @Column(name = "category_icon")
    private String categoryIcon;

    @Column(name = "icon_url")
    private String iconUrl;

    @Column(name = "icon_type")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private IconType iconType = IconType.svg;

    public enum IconType {
        svg, png, jpg
    }
}