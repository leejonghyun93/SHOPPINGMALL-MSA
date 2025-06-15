package org.kosa.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserGradeDto {

    private String gradeId;
    private String gradeName;
    private Integer gradeMinAmount;
    private BigDecimal gradePointRate;
    private BigDecimal gradeDiscountRate;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
}