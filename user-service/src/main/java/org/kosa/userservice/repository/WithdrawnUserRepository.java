package org.kosa.userservice.repository;

import org.kosa.userservice.entity.WithdrawnUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WithdrawnUserRepository extends JpaRepository<WithdrawnUser, Long> {
}