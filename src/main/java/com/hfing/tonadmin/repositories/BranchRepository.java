package com.hfing.tonadmin.repositories;

import com.hfing.tonadmin.entities.Branch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BranchRepository extends JpaRepository<Branch, String> {

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, String id);

    List<Branch> findAllByOrderByCreatedAtDesc();
}