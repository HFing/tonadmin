package com.hfing.tonadmin.services;

import com.hfing.tonadmin.dto.request.InventoryImportRequest;
import com.hfing.tonadmin.dto.response.StockTransactionSummaryProjection;
import com.hfing.tonadmin.entities.Inventory;
import com.hfing.tonadmin.entities.StockTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.BindingResult;

import java.util.List;

public interface InventoryService {

    List<Inventory> getAllInventories();

    List<Inventory> getInventories(String branchId, String productId);

    Page<Inventory> getInventoryPage(String branchId, String productId, Pageable pageable);

    List<StockTransaction> getAllTransactions();

    List<StockTransaction> getTransactions(String branchId, String productId);

    Page<StockTransactionSummaryProjection> getTransactionSummaryPage(Pageable pageable);

    boolean importInventory(InventoryImportRequest request, BindingResult bindingResult);
}