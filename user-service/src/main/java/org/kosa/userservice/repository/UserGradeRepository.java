package org.kosa.userservice.repository;

import org.kosa.userservice.entity.MemberGrade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserGradeRepository extends JpaRepository<MemberGrade, String> {

    // 최소 금액이 가장 낮은 등급 찾기 (신규 회원용)
    Optional<MemberGrade> findTopByOrderByGradeMinAmountAsc();

    // 모든 등급을 최소 금액 순으로 정렬
    List<MemberGrade> findAllByOrderByGradeMinAmountAsc();

    // 구매 금액 이하의 등급 중 최고 등급 찾기 (관리자/호스트 제외)
    @Query("SELECT mg FROM MemberGrade mg WHERE mg.gradeMinAmount <= :purchaseAmount " +
            "AND mg.gradeName NOT LIKE '%관리자%' AND mg.gradeName NOT LIKE '%ADMIN%' " +
            "AND mg.gradeName NOT LIKE '%호스트%' AND mg.gradeName NOT LIKE '%HOST%' " +
            "ORDER BY mg.gradeMinAmount DESC LIMIT 1")
    Optional<MemberGrade> findBestGradeByPurchaseAmount(@Param("purchaseAmount") Integer purchaseAmount);

    // 특정 금액 범위의 등급들 조회
    @Query("SELECT mg FROM MemberGrade mg WHERE mg.gradeMinAmount BETWEEN :minAmount AND :maxAmount ORDER BY mg.gradeMinAmount")
    List<MemberGrade> findGradesByAmountRange(@Param("minAmount") Integer minAmount, @Param("maxAmount") Integer maxAmount);

    //  특정 할인율 이상의 등급들 조회
    @Query("SELECT mg FROM MemberGrade mg WHERE mg.gradeDiscountRate >= :minDiscountRate ORDER BY mg.gradeDiscountRate DESC")
    List<MemberGrade> findGradesByMinDiscountRate(@Param("minDiscountRate") java.math.BigDecimal minDiscountRate);

    //  특정 포인트 적립률 이상의 등급들 조회
    @Query("SELECT mg FROM MemberGrade mg WHERE mg.gradePointRate >= :minPointRate ORDER BY mg.gradePointRate DESC")
    List<MemberGrade> findGradesByMinPointRate(@Param("minPointRate") java.math.BigDecimal minPointRate);

    //  등급명으로 검색 (부분 일치)
    @Query("SELECT mg FROM MemberGrade mg WHERE mg.gradeName LIKE %:gradeName% ORDER BY mg.gradeMinAmount")
    List<MemberGrade> findGradesByNameContaining(@Param("gradeName") String gradeName);

    // 고객 등급만 조회 (관리자/호스트 제외)
    @Query("SELECT mg FROM MemberGrade mg WHERE mg.gradeName NOT LIKE '%관리자%' AND mg.gradeName NOT LIKE '%ADMIN%' " +
            "AND mg.gradeName NOT LIKE '%호스트%' AND mg.gradeName NOT LIKE '%HOST%' ORDER BY mg.gradeMinAmount")
    List<MemberGrade> findCustomerGrades();

    //  특별 등급만 조회 (관리자/호스트)
    @Query("SELECT mg FROM MemberGrade mg WHERE mg.gradeName LIKE '%관리자%' OR mg.gradeName LIKE '%ADMIN%' " +
            "OR mg.gradeName LIKE '%호스트%' OR mg.gradeName LIKE '%HOST%'")
    List<MemberGrade> findSpecialGrades();
}