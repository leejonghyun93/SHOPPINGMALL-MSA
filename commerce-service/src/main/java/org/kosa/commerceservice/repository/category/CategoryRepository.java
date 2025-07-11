package org.kosa.commerceservice.repository.category;

import org.kosa.commerceservice.entity.category.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {

    // 메인 카테고리 조회 (최상위 카테고리만, parentCategory가 null인 것들)
    List<Category> findByParentCategoryIsNullAndCategoryUseYnOrderByCategoryDisplayOrder(String useYn);

    // 특정 카테고리의 하위 카테고리 조회 (서브 카테고리 목록)
    List<Category> findByParentCategory_CategoryIdAndCategoryUseYnOrderByCategoryDisplayOrder(Integer parentCategoryId, String useYn);

    // 카테고리 레벨별 조회 (1레벨=메인, 2레벨=서브 등)
    List<Category> findByCategoryLevelAndCategoryUseYnOrderByCategoryDisplayOrder(Integer categoryLevel, String categoryUseYn);

    // 특정 카테고리와 그 하위 카테고리 모두 조회 (상품 필터링용)
    @Query("SELECT c FROM Category c WHERE c.categoryId = :categoryId OR c.parentCategory.categoryId = :categoryId")
    List<Category> findCategoryWithSubCategories(@Param("categoryId") Integer categoryId);

    // 전체 카테고리 계층구조별 조회 (트리 구조로 정렬)
    List<Category> findByCategoryUseYnOrderByCategoryLevelAscCategoryDisplayOrderAsc(String useYn);
}