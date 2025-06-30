package org.kosa.imageservice.repository;

import org.kosa.imageservice.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductImageRepository extends JpaRepository<ProductImage, Integer> {  // String → Integer

    // productId 타입을 Integer로 변경
    List<ProductImage> findByProductIdOrderByImageSeqAsc(Integer productId);  // String → Integer

    Optional<ProductImage> findByProductIdAndIsMainImage(Integer productId, String isMainImage);  // String → Integer
}