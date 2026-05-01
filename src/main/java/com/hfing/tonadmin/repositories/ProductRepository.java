package com.hfing.tonadmin.repositories;

import com.hfing.tonadmin.entities.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, String> {

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, String id);

    @EntityGraph(attributePaths = {"category"})
    Page<Product> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"category"})
    List<Product> findAllByOrderByCreatedAtDesc();

    List<Product> findByActiveTrueOrderByNameAsc();
}