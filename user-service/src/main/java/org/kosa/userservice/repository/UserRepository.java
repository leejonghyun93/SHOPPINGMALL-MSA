package org.kosa.userservice.repository;


import org.kosa.userservice.dto.UserDto;
import org.kosa.userservice.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<Member, String> {

    Optional<Member> findByUserId(String userId);
    boolean existsByUserId(String userId);

    Optional<Member> findByNameAndEmail(String name, String email);
    Optional<Member> findByEmail(String email);
    Optional<Member> findByNameAndEmailAndStatusAndSecessionYn(
            String name,
            String email,
            String status,
            String secessionYn
    );
    // 탈퇴하지 않은 활성 회원만 조회
    @Query("SELECT m FROM Member m WHERE m.userId = :userId AND m.secessionYn = 'N'")
    Optional<Member> findActiveByUserId(@Param("userId") String userId);

    // 블랙리스트가 아닌 회원만 조회
    @Query("SELECT m FROM Member m WHERE m.userId = :userId AND m.blacklisted = 'N'")
    Optional<Member> findNonBlacklistedByUserId(@Param("userId") String userId);

    //  등급별 회원 수 조회
    @Query("SELECT COUNT(m) FROM Member m WHERE m.memberGrade.gradeId = :gradeId")
    long countByMemberGradeGradeId(@Param("gradeId") String gradeId);

    //  특정 등급의 회원 조회
    @Query("SELECT m FROM Member m WHERE m.memberGrade.gradeId = :gradeId")
    List<Member> findByMemberGradeGradeId(@Param("gradeId") String gradeId);

    //  특정 상태의 회원 수 조회
    long countByStatus(String status);

    //  마케팅 동의한 회원 조회
    @Query("SELECT m FROM Member m WHERE m.marketingAgree = 'Y' AND m.secessionYn = 'N'")
    List<Member> findMarketingAgreedMembers();

    //  활성 회원 수 조회
    @Query("SELECT COUNT(m) FROM Member m WHERE m.status = 'ACTIVE' AND m.secessionYn = 'N'")
    long countActiveMembers();

    //  등급별 활성 회원 조회
    @Query("SELECT m FROM Member m WHERE m.memberGrade.gradeId = :gradeId AND m.status = 'ACTIVE' AND m.secessionYn = 'N'")
    List<Member> findActiveByMemberGradeGradeId(@Param("gradeId") String gradeId);
}