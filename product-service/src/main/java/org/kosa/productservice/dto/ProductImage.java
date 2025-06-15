package org.kosa.productservice.dto;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_product_image")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductImage {

    @Id
    @Column(name = "IMAGE_ID")
    private String imageId;

    @Column(name = "PRODUCT_ID", nullable = false)
    private String productId;

    @Column(name = "IMAGE_URL", nullable = false)
    private String imageUrl;

    @Column(name = "IMAGE_SEQ", nullable = false)
    private Integer imageSeq;

    @Column(name = "IMAGE_NAME_YN")
    private String imageNameYn;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;
}