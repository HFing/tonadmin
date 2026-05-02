package com.hfing.tonadmin.repositories;

import com.hfing.tonadmin.entities.StockTransferItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockTransferItemRepository extends JpaRepository<StockTransferItem, String> {

    @EntityGraph(attributePaths = {"product", "product.category"})
    List<StockTransferItem> findByStockTransferId(String stockTransferId);
}