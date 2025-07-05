package org.kosa.commerceservice.repository.category;

import org.kosa.commerceservice.entity.category.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {

    List<Category> findByParentCategoryIsNullAndCategoryUseYnOrderByCategoryDisplayOrder(String useYn);

    List<Category> findByParentCategory_CategoryIdAndCategoryUseYnOrderByCategoryDisplayOrder(Integer parentCategoryId, String useYn);

    List<Category> findByCategoryLevelAndCategoryUseYnOrderByCategoryDisplayOrder(Integer categoryLevel, String categoryUseYn);

    @Query("SELECT c FROM Category c WHERE c.categoryId = :categoryId OR c.parentCategory.categoryId = :categoryId")
    List<Category> findCategoryWithSubCategories(@Param("categoryId") Integer categoryId);

    List<Category> findByCategoryUseYnOrderByCategoryLevelAscCategoryDisplayOrderAsc(String useYn);
}
