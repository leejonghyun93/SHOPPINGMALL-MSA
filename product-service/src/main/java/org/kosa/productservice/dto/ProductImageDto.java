package org.kosa.productservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductImageDto {
    private String imageId;
    private String productId;
    private String imageUrl;
    private Integer imageSeq;
    private String imageNameYn;
    private LocalDateTime createdDate;
}