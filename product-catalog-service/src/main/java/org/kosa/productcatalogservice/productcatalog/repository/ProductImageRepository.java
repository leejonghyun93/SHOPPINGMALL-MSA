package org.kosa.productcatalogservice.productcatalog.repository;


import org.kosa.productcatalogservice.productcatalog.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, Integer> {

    List<ProductImage> findByProductIdOrderByImageSeqAsc(Integer productId);

    Optional<ProductImage> findByProductIdAndIsMainImage(Integer productId, String isMainImage);
}