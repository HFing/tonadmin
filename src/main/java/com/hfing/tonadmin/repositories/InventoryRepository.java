package com.hfing.tonadmin.repositories;

import com.hfing.tonadmin.entities.Branch;
import com.hfing.tonadmin.entities.Inventory;
import com.hfing.tonadmin.entities.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, String>, JpaSpecificationExecutor<Inventory> {

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

    @Override
    @EntityGraph(attributePaths = {"branch", "product", "product.category"})
    Page<Inventory> findAll(Specification<Inventory> specification, Pageable pageable);

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

    @Query("""
        select count(i)
        from Inventory i
        where i.product.active = true
          and i.quantity > 0
          and i.quantity <= i.product.minStock
        """)
    long countLowStock();

    @Query("""
        select count(i)
        from Inventory i
        where i.product.active = true
          and i.branch.id = :branchId
          and i.quantity > 0
          and i.quantity <= i.product.minStock
        """)
    long countLowStockByBranch(@Param("branchId") String branchId);

    @Query("""
        select count(i)
        from Inventory i
        where i.product.active = true
          and i.quantity <= 0
        """)
    long countOutOfStock();

    @Query("""
        select count(i)
        from Inventory i
        where i.product.active = true
          and i.branch.id = :branchId
          and i.quantity <= 0
        """)
    long countOutOfStockByBranch(@Param("branchId") String branchId);

    @EntityGraph(attributePaths = {"branch", "product", "product.category"})
    @Query("""
        select i
        from Inventory i
        where i.product.active = true
          and i.quantity > 0
          and i.quantity <= i.product.minStock
        order by i.quantity asc
        """)
    List<Inventory> findTopLowStock(Pageable pageable);

    @EntityGraph(attributePaths = {"branch", "product", "product.category"})
    @Query("""
        select i
        from Inventory i
        where i.product.active = true
          and i.branch.id = :branchId
          and i.quantity > 0
          and i.quantity <= i.product.minStock
        order by i.quantity asc
        """)
    List<Inventory> findTopLowStockByBranch(@Param("branchId") String branchId, Pageable pageable);
}
