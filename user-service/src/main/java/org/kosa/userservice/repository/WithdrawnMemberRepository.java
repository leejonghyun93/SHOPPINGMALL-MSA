package org.kosa.userservice.repository;

import org.kosa.userservice.entity.WithdrawnMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface WithdrawnMemberRepository extends JpaRepository<WithdrawnMember, String> {

    Optional<WithdrawnMember> findByUserId(String userId);

    List<WithdrawnMember> findByWithdrawnWithdrawnDateBetween(
            LocalDate startDate, LocalDate endDate);

    boolean existsByUserId(String userId);

    @Query("SELECT COUNT(w) FROM WithdrawnMember w WHERE w.withdrawnWithdrawnDate = :date")
    long countByWithdrawnDate(@Param("date") LocalDate date);
}