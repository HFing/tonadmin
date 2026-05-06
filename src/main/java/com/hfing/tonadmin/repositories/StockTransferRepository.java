package com.hfing.tonadmin.repositories;

import com.hfing.tonadmin.entities.StockTransfer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;

public interface StockTransferRepository extends JpaRepository<StockTransfer, String>, JpaSpecificationExecutor<StockTransfer> {

    @EntityGraph(attributePaths = {"sourceBranch", "targetBranch"})
    Page<StockTransfer> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"sourceBranch", "targetBranch"})
    Page<StockTransfer> findAll(Specification<StockTransfer> specification, Pageable pageable);
}
