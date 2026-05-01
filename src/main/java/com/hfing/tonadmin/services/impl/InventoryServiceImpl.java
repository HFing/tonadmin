package com.hfing.tonadmin.services.impl;

import com.hfing.tonadmin.common.StockTransactionType;
import com.hfing.tonadmin.dto.request.InventoryImportItemRequest;
import com.hfing.tonadmin.dto.request.InventoryImportRequest;
import com.hfing.tonadmin.dto.response.StockTransactionSummaryProjection;
import com.hfing.tonadmin.entities.Branch;
import com.hfing.tonadmin.entities.Inventory;
import com.hfing.tonadmin.entities.Product;
import com.hfing.tonadmin.entities.StockTransaction;
import com.hfing.tonadmin.repositories.BranchRepository;
import com.hfing.tonadmin.repositories.InventoryRepository;
import com.hfing.tonadmin.repositories.ProductRepository;
import com.hfing.tonadmin.repositories.StockTransactionRepository;
import com.hfing.tonadmin.services.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final StockTransactionRepository stockTransactionRepository;
    private final BranchRepository branchRepository;
    private final ProductRepository productRepository;

    @Override
    public List<Inventory> getAllInventories() {
        return inventoryRepository.findAllByOrderByUpdatedAtDesc();
    }

    @Override
    public List<Inventory> getInventories(String branchId, String productId) {
        boolean hasBranch = branchId != null && !branchId.isBlank();
        boolean hasProduct = productId != null && !productId.isBlank();

        if (hasBranch && hasProduct) {
            return inventoryRepository.findByBranchIdAndProductIdOrderByProductNameAsc(branchId, productId);
        }

        if (hasBranch) {
            return inventoryRepository.findByBranchIdOrderByProductNameAsc(branchId);
        }

        if (hasProduct) {
            return inventoryRepository.findByProductIdOrderByBranchNameAsc(productId);
        }

        return inventoryRepository.findAllByOrderByUpdatedAtDesc();
    }

    @Override
    public Page<Inventory> getInventoryPage(String branchId, String productId, Pageable pageable) {
        boolean hasBranch = branchId != null && !branchId.isBlank();
        boolean hasProduct = productId != null && !productId.isBlank();

        if (hasBranch && hasProduct) {
            return inventoryRepository.findByBranchIdAndProductIdOrderByProductNameAsc(
                    branchId,
                    productId,
                    pageable
            );
        }

        if (hasBranch) {
            return inventoryRepository.findByBranchIdOrderByProductNameAsc(branchId, pageable);
        }

        if (hasProduct) {
            return inventoryRepository.findByProductIdOrderByBranchNameAsc(productId, pageable);
        }

        return inventoryRepository.findAllByOrderByUpdatedAtDesc(pageable);
    }

    @Override
    public List<StockTransaction> getAllTransactions() {
        return stockTransactionRepository.findAllByOrderByCreatedAtDesc();
    }

    @Override
    public List<StockTransaction> getTransactions(String branchId, String productId) {
        boolean hasBranch = branchId != null && !branchId.isBlank();
        boolean hasProduct = productId != null && !productId.isBlank();

        if (hasBranch && hasProduct) {
            return stockTransactionRepository.findByBranchIdAndProductIdOrderByCreatedAtDesc(branchId, productId);
        }

        if (hasBranch) {
            return stockTransactionRepository.findByBranchIdOrderByCreatedAtDesc(branchId);
        }

        if (hasProduct) {
            return stockTransactionRepository.findByProductIdOrderByCreatedAtDesc(productId);
        }

        return stockTransactionRepository.findAllByOrderByCreatedAtDesc();
    }

    @Override
    public Page<StockTransactionSummaryProjection> getTransactionSummaryPage(Pageable pageable) {
        return stockTransactionRepository.findTransactionSummaries(pageable);
    }

    @Override
    @Transactional
    public boolean importInventory(InventoryImportRequest request, BindingResult bindingResult) {
        Branch branch = branchRepository.findById(request.branchId()).orElse(null);

        if (branch == null || !Boolean.TRUE.equals(branch.getActive())) {
            bindingResult.rejectValue("branchId", "invalid", "Chi nhánh không hợp lệ hoặc đã bị khóa");
            return false;
        }

        if (request.items() == null || request.items().isEmpty()) {
            bindingResult.rejectValue("items", "required", "Vui lòng thêm ít nhất một sản phẩm");
            return false;
        }

        boolean hasError = validateImportItems(request, bindingResult);

        if (hasError) {
            return false;
        }

        List<String> productIds = request.items()
                .stream()
                .map(InventoryImportItemRequest::productId)
                .toList();

        Map<String, Product> productMap = productRepository.findAllById(productIds)
                .stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        hasError = validateProductsExistAndActive(request, productMap, bindingResult);

        if (hasError) {
            return false;
        }

        String batchCode = generateBatchCode();
        String batchNote = trimToNull(request.note());

        for (InventoryImportItemRequest item : request.items()) {
            Product product = productMap.get(item.productId());

            Inventory inventory = inventoryRepository.findWithLockByBranchAndProduct(branch, product)
                    .orElseGet(() -> Inventory.builder()
                            .branch(branch)
                            .product(product)
                            .quantity(BigDecimal.ZERO)
                            .build());

            BigDecimal beforeQuantity = inventory.getQuantity() == null
                    ? BigDecimal.ZERO
                    : inventory.getQuantity();

            BigDecimal importQuantity = item.quantity();
            BigDecimal afterQuantity = beforeQuantity.add(importQuantity);

            inventory.setQuantity(afterQuantity);
            inventoryRepository.save(inventory);

            StockTransaction transaction = StockTransaction.builder()
                    .batchCode(batchCode)
                    .batchNote(batchNote)
                    .branch(branch)
                    .product(product)
                    .transactionType(StockTransactionType.IMPORT)
                    .quantity(importQuantity)
                    .beforeQuantity(beforeQuantity)
                    .afterQuantity(afterQuantity)
                    .note(batchNote)
                    .build();

            stockTransactionRepository.save(transaction);
        }

        return true;
    }

    private boolean validateImportItems(
            InventoryImportRequest request,
            BindingResult bindingResult
    ) {
        boolean hasError = false;
        Set<String> productIds = new HashSet<>();

        for (int i = 0; i < request.items().size(); i++) {
            InventoryImportItemRequest item = request.items().get(i);

            if (item.productId() == null || item.productId().isBlank()) {
                bindingResult.rejectValue(
                        "items[" + i + "].productId",
                        "required",
                        "Vui lòng chọn sản phẩm"
                );
                hasError = true;
                continue;
            }

            if (!productIds.add(item.productId())) {
                bindingResult.rejectValue(
                        "items[" + i + "].productId",
                        "duplicate",
                        "Sản phẩm này đã được chọn ở dòng khác"
                );
                hasError = true;
            }

            if (item.quantity() == null || item.quantity().compareTo(BigDecimal.ONE) < 0) {
                bindingResult.rejectValue(
                        "items[" + i + "].quantity",
                        "invalid",
                        "Số lượng phải từ 1 trở lên"
                );
                hasError = true;
            }
        }

        return hasError;
    }

    private boolean validateProductsExistAndActive(
            InventoryImportRequest request,
            Map<String, Product> productMap,
            BindingResult bindingResult
    ) {
        boolean hasError = false;

        for (int i = 0; i < request.items().size(); i++) {
            InventoryImportItemRequest item = request.items().get(i);

            if (item.productId() == null || item.productId().isBlank()) {
                continue;
            }

            Product product = productMap.get(item.productId());

            if (product == null) {
                bindingResult.rejectValue(
                        "items[" + i + "].productId",
                        "notFound",
                        "Không tìm thấy sản phẩm"
                );
                hasError = true;
                continue;
            }

            if (!Boolean.TRUE.equals(product.getActive())) {
                bindingResult.rejectValue(
                        "items[" + i + "].productId",
                        "inactive",
                        "Sản phẩm đã bị khóa"
                );
                hasError = true;
            }
        }

        return hasError;
    }

    private String generateBatchCode() {
        return "IMP" + LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        return value.trim();
    }
}