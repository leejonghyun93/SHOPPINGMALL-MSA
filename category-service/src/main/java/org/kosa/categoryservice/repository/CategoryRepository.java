package org.kosa.categoryservice.repository;

import org.kosa.categoryservice.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {

    // 최상위 카테고리 조회 (레벨 1)
    List<Category> findByParentCategoryIsNullAndCategoryUseYnOrderByCategoryDisplayOrder(String useYn);

    // 부모 카테고리 ID로 하위 카테고리 조회
    List<Category> findByParentCategory_CategoryIdAndCategoryUseYnOrderByCategoryDisplayOrder(Integer parentCategoryId, String useYn);

    // 카테고리 레벨별 조회
    List<Category> findByCategoryLevelAndCategoryUseYnOrderByCategoryDisplayOrder(
            Integer categoryLevel, String categoryUseYn
    );

    // 카테고리 ID로 하위 카테고리 포함 조회
    @Query("SELECT c FROM Category c WHERE c.categoryId = :categoryId OR c.parentCategory.categoryId = :categoryId")
    List<Category> findCategoryWithSubCategories(@Param("categoryId") Integer categoryId);

    // 모든 활성 카테고리 조회 (계층 구조용)
    List<Category> findByCategoryUseYnOrderByCategoryLevelAscCategoryDisplayOrderAsc(String useYn);
}