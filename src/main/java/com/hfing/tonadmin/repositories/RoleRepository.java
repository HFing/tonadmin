package com.hfing.tonadmin.repositories;

import com.hfing.tonadmin.entities.Role;
import com.hfing.tonadmin.common.RoleType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, String> {

    Optional<Role> findByRoleType(RoleType roleType);

    boolean existsByRoleType(RoleType roleType);
}