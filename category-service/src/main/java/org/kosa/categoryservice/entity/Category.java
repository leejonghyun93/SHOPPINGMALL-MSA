package org.kosa.categoryservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;



@Entity
@Table(name = "tb_category")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"parentCategory", "subCategories"})
public class Category {

    @Id
    @Column(name = "CATEGORY_ID")
    private String categoryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PARENT_CATEGORY_ID")
    private Category parentCategory;

    @OneToMany(mappedBy = "parentCategory", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Category> subCategories = new ArrayList<>();

    @Column(name = "NAME", nullable = false)
    private String name;

    @Column(name = "CATEGORY_LEVEL")
    private Integer categoryLevel;

    @Column(name = "CATEGORY_DISPLAY_ORDER")
    private Integer categoryDisplayOrder;

    @Column(name = "CATEGORY_USE_YN")
    @Builder.Default
    private String categoryUseYn = "Y";

    @Column(name = "category_icon")
    private String categoryIcon;

    @CreationTimestamp
    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @UpdateTimestamp
    @Column(name = "UPDATED_DATE")
    private LocalDateTime updatedDate;
}