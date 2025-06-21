package org.kosa.imageservice.repository;

import org.kosa.imageservice.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.List;
import java.util.Optional;

public interface ProductImageRepository extends JpaRepository<ProductImage, String> {
    List<ProductImage> findByProductIdOrderByImageSeqAsc(String productId);
    Optional<ProductImage> findByProductIdAndIsMainImage(String productId, String isMainImage);
}