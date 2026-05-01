package com.hfing.tonadmin.repositories;

import com.hfing.tonadmin.entities.Branch;
import com.hfing.tonadmin.entities.Inventory;
import com.hfing.tonadmin.entities.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, String> {

    Optional<Inventory> findByBranchAndProduct(Branch branch, Product product);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Inventory> findWithLockByBranchAndProduct(Branch branch, Product product);

    @EntityGraph(attributePaths = {"branch", "product", "product.category"})
    List<Inventory> findAllByOrderByUpdatedAtDesc();

    @EntityGraph(attributePaths = {"branch", "product", "product.category"})
    List<Inventory> findByBranchIdOrderByProductNameAsc(String branchId);

    @EntityGraph(attributePaths = {"branch", "product", "product.category"})
    List<Inventory> findByProductIdOrderByBranchNameAsc(String productId);

    @EntityGraph(attributePaths = {"branch", "product", "product.category"})
    List<Inventory> findByBranchIdAndProductIdOrderByProductNameAsc(String branchId, String productId);

    @EntityGraph(attributePaths = {"branch", "product", "product.category"})
    Page<Inventory> findAllByOrderByUpdatedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"branch", "product", "product.category"})
    Page<Inventory> findByBranchIdOrderByProductNameAsc(String branchId, Pageable pageable);

    @EntityGraph(attributePaths = {"branch", "product", "product.category"})
    Page<Inventory> findByProductIdOrderByBranchNameAsc(String productId, Pageable pageable);

    @EntityGraph(attributePaths = {"branch", "product", "product.category"})
    Page<Inventory> findByBranchIdAndProductIdOrderByProductNameAsc(
            String branchId,
            String productId,
            Pageable pageable
    );
}