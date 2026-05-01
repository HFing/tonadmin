package com.hfing.tonadmin.repositories;


import com.hfing.tonadmin.entities.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    @EntityGraph(attributePaths = {"userHasRoles", "userHasRoles.role"})
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, String id);

    @EntityGraph(attributePaths = {"branch", "userHasRoles", "userHasRoles.role"})
    List<User> findAllByOrderByCreatedAtDesc();
}
