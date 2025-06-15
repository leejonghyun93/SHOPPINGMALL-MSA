package org.kosa.userservice.repository;

import org.kosa.userservice.entity.WithdrawnMember;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WithdrawnUserRepository extends JpaRepository<WithdrawnMember, Long> {
}