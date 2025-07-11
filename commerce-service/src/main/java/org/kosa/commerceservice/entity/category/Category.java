package org.kosa.commerceservice.entity.category;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tb_category")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@ToString(exclude = {"parentCategory", "subCategories"})
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private Integer categoryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_category_id")
    private Category parentCategory;

    @OneToMany(mappedBy = "parentCategory", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Category> subCategories = new ArrayList<>();

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "category_level")
    @Builder.Default
    private Integer categoryLevel = 1;

    @Column(name = "category_display_order")
    private Integer categoryDisplayOrder;

    @Column(name = "category_use_yn", length = 1, columnDefinition = "CHAR(1)")
    @Builder.Default
    private String categoryUseYn = "Y";

    @Column(name = "category_icon")
    private String categoryIcon;

    @Column(name = "icon_url", length = 500)
    private String iconUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "icon_type", columnDefinition = "ENUM('svg','png','jpg') DEFAULT 'svg'")
    @Builder.Default
    private IconType iconType = IconType.svg;

    @CreationTimestamp
    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @UpdateTimestamp
    @Column(name = "updated_date")
    private LocalDateTime updatedDate;

    public enum IconType {
        svg, png, jpg
    }
}