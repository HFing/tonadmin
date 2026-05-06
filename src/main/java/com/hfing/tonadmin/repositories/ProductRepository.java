package com.hfing.tonadmin.repositories;

import com.hfing.tonadmin.entities.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, String>, JpaSpecificationExecutor<Product> {

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, String id);

    @EntityGraph(attributePaths = {"category"})
    Page<Product> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"category"})
    Page<Product> findAll(Specification<Product> specification, Pageable pageable);

    @EntityGraph(attributePaths = {"category"})
    List<Product> findAllByOrderByCreatedAtDesc();

    List<Product> findByActiveTrueOrderByNameAsc();
}
