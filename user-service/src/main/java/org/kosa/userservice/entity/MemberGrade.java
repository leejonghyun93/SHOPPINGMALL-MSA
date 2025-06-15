package org.kosa.userservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_member_grade")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberGrade {

    @Id
    @Column(name = "GRADE_ID", length = 20)
    private String gradeId;

    @Column(name = "GRADE_NAME", nullable = false, length = 50)
    private String gradeName;

    @Column(name = "GRADE_MIN_AMOUNT")
    @Builder.Default
    private Integer gradeMinAmount = 0;

    @Column(name = "GRADE_POINT_RATE", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal gradePointRate = BigDecimal.ZERO;

    @Column(name = "GRADE_DISCOUNT_RATE", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal gradeDiscountRate = BigDecimal.ZERO;

    @CreationTimestamp
    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @UpdateTimestamp
    @Column(name = "UPDATED_DATE")
    private LocalDateTime updatedDate;

    // 편의 메서드
    public boolean isAdminGrade() {
        return "ADMIN".equals(this.gradeId);
    }

    public boolean isHostGrade() {
        return "HOST".equals(this.gradeId);
    }

    public boolean isCustomerGrade() {
        return !isAdminGrade() && !isHostGrade();
    }

    // 등급별 권한 체크
    public boolean hasManagementPermission() {
        return isAdminGrade() || isHostGrade();
    }

    // 할인율을 퍼센트로 반환
    public double getDiscountPercentage() {
        return gradeDiscountRate.doubleValue();
    }

    // 포인트 적립률을 퍼센트로 반환
    public double getPointPercentage() {
        return gradePointRate.doubleValue();
    }
}