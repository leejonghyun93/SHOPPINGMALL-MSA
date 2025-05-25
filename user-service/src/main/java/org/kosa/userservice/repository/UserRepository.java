package org.kosa.userservice.repository;


import org.kosa.userservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByUserid(String userid);
    boolean existsByUserid(String userid);

    // userid 리스트로 사용자 목록 조회
    List<User> findByUseridIn(List<String> userids);

}