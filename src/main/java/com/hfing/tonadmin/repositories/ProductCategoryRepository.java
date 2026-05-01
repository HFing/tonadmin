package com.hfing.tonadmin.repositories;

import com.hfing.tonadmin.entities.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductCategoryRepository extends JpaRepository<ProductCategory, String> {

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, String id);

    List<ProductCategory> findAllByOrderByCreatedAtDesc();

    List<ProductCategory> findByActiveTrueOrderByNameAsc();
}
