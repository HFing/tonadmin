package com.hfing.tonadmin.repositories;

import com.hfing.tonadmin.entities.StockTransfer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockTransferRepository extends JpaRepository<StockTransfer, String> {

    @EntityGraph(attributePaths = {"sourceBranch", "targetBranch"})
    Page<StockTransfer> findAllByOrderByCreatedAtDesc(Pageable pageable);
}