package org.kosa.userservice.repository;


import org.kosa.userservice.dto.PageDto;
import org.kosa.userservice.dto.UserDto;
import org.kosa.userservice.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByUserid(String userid);
    boolean existsByUserid(String userid);

    Page<User> findByUseridIn(List<String> userids, Pageable pageable);

    @Query("SELECT u FROM User u " +
            "WHERE (:searchValue IS NULL OR " +
            "LOWER(u.userid) LIKE LOWER(CONCAT('%', :searchValue, '%')) OR " +
            "LOWER(u.name) LIKE LOWER(CONCAT('%', :searchValue, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :searchValue, '%')) )")
    Page<User> searchUsers(@Param("searchValue") String searchValue, Pageable pageable);

    Optional<User> findByNameAndEmail(String name, String email);

}