package com.hfing.tonadmin.services.impl;

import com.hfing.tonadmin.common.StockTransactionType;
import com.hfing.tonadmin.dto.request.StockTransferItemRequest;
import com.hfing.tonadmin.dto.request.StockTransferRequest;
import com.hfing.tonadmin.dto.request.StockTransferSearchRequest;
import com.hfing.tonadmin.entities.*;
import com.hfing.tonadmin.repositories.*;
import com.hfing.tonadmin.services.NotificationService;
import com.hfing.tonadmin.services.StockTransferService;
import com.hfing.tonadmin.specifications.StockTransferSpecifications;
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
public class StockTransferServiceImpl implements StockTransferService {

    private final StockTransferRepository stockTransferRepository;
    private final StockTransferItemRepository stockTransferItemRepository;
    private final BranchRepository branchRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final StockTransactionRepository stockTransactionRepository;
    private final NotificationService notificationService;

    @Override
    public Page<StockTransfer> getTransfers(StockTransferSearchRequest search, Pageable pageable) {
        StockTransferSearchRequest safeSearch = search == null
                ? new StockTransferSearchRequest(null, null, null, null, null)
                : search;

        return stockTransferRepository.findAll(StockTransferSpecifications.bySearch(safeSearch), pageable);
    }

    @Override
    public StockTransfer getTransferById(String id) {
        return stockTransferRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu chuyển kho"));
    }

    @Override
    public List<StockTransferItem> getTransferItems(String transferId) {
        return stockTransferItemRepository.findByStockTransferId(transferId);
    }

    @Override
    @Transactional
    public StockTransfer createTransfer(StockTransferRequest request, BindingResult bindingResult) {
        Branch sourceBranch = branchRepository.findById(request.sourceBranchId()).orElse(null);
        Branch targetBranch = branchRepository.findById(request.targetBranchId()).orElse(null);

        if (sourceBranch == null || !Boolean.TRUE.equals(sourceBranch.getActive())) {
            bindingResult.rejectValue("sourceBranchId", "invalid", "Chi nhánh xuất không hợp lệ hoặc đã bị khóa");
            return null;
        }

        if (targetBranch == null || !Boolean.TRUE.equals(targetBranch.getActive())) {
            bindingResult.rejectValue("targetBranchId", "invalid", "Chi nhánh nhận không hợp lệ hoặc đã bị khóa");
            return null;
        }

        if (sourceBranch.getId().equals(targetBranch.getId())) {
            bindingResult.rejectValue("targetBranchId", "sameBranch", "Chi nhánh nhận phải khác chi nhánh xuất");
            return null;
        }

        if (request.items() == null || request.items().isEmpty()) {
            bindingResult.rejectValue("items", "required", "Vui lòng thêm ít nhất một sản phẩm");
            return null;
        }

        boolean hasError = validateItems(request, bindingResult);
        if (hasError) {
            return null;
        }

        List<String> productIds = request.items()
                .stream()
                .map(StockTransferItemRequest::productId)
                .toList();

        Map<String, Product> productMap = productRepository.findAllById(productIds)
                .stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        hasError = validateProducts(request, productMap, bindingResult);
        if (hasError) {
            return null;
        }

        String transferCode = generateTransferCode();
        String transferNote = trimToNull(request.note());

        StockTransfer stockTransfer = StockTransfer.builder()
                .transferCode(transferCode)
                .sourceBranch(sourceBranch)
                .targetBranch(targetBranch)
                .note(transferNote)
                .build();

        for (int i = 0; i < request.items().size(); i++) {
            StockTransferItemRequest itemRequest = request.items().get(i);
            Product product = productMap.get(itemRequest.productId());
            BigDecimal transferQuantity = itemRequest.quantity();

            Inventory sourceInventory = inventoryRepository
                    .findWithLockByBranchAndProduct(sourceBranch, product)
                    .orElse(null);

            if (sourceInventory == null) {
                bindingResult.rejectValue(
                        "items[" + i + "].productId",
                        "noStock",
                        "Sản phẩm chưa có tồn kho tại chi nhánh xuất"
                );
                return null;
            }

            BigDecimal sourceBefore = zeroIfNull(sourceInventory.getQuantity());

            if (sourceBefore.compareTo(transferQuantity) < 0) {
                bindingResult.rejectValue(
                        "items[" + i + "].quantity",
                        "insufficientStock",
                        "Tồn kho chi nhánh xuất không đủ. Hiện còn: " + sourceBefore
                );
                return null;
            }

            BigDecimal sourceAfter = sourceBefore.subtract(transferQuantity);
            sourceInventory.setQuantity(sourceAfter);
            inventoryRepository.save(sourceInventory);
            notificationService.notifyStockStatusIfNeeded(sourceInventory);

            Inventory targetInventory = inventoryRepository
                    .findWithLockByBranchAndProduct(targetBranch, product)
                    .orElseGet(() -> Inventory.builder()
                            .branch(targetBranch)
                            .product(product)
                            .quantity(BigDecimal.ZERO)
                            .build());

            BigDecimal targetBefore = zeroIfNull(targetInventory.getQuantity());
            BigDecimal targetAfter = targetBefore.add(transferQuantity);
            targetInventory.setQuantity(targetAfter);
            inventoryRepository.save(targetInventory);
            notificationService.notifyStockStatusIfNeeded(targetInventory);

            StockTransferItem transferItem = StockTransferItem.builder()
                    .stockTransfer(stockTransfer)
                    .product(product)
                    .quantity(transferQuantity)
                    .build();

            stockTransfer.getItems().add(transferItem);

            stockTransactionRepository.save(StockTransaction.builder()
                    .batchCode(transferCode)
                    .batchNote(transferNote)
                    .branch(sourceBranch)
                    .product(product)
                    .transactionType(StockTransactionType.TRANSFER_OUT)
                    .quantity(transferQuantity)
                    .beforeQuantity(sourceBefore)
                    .afterQuantity(sourceAfter)
                    .note(transferNote)
                    .build());

            stockTransactionRepository.save(StockTransaction.builder()
                    .batchCode(transferCode)
                    .batchNote(transferNote)
                    .branch(targetBranch)
                    .product(product)
                    .transactionType(StockTransactionType.TRANSFER_IN)
                    .quantity(transferQuantity)
                    .beforeQuantity(targetBefore)
                    .afterQuantity(targetAfter)
                    .note(transferNote)
                    .build());
        }

        return stockTransferRepository.save(stockTransfer);
    }

    private boolean validateItems(StockTransferRequest request, BindingResult bindingResult) {
        boolean hasError = false;
        Set<String> productIds = new HashSet<>();

        for (int i = 0; i < request.items().size(); i++) {
            StockTransferItemRequest item = request.items().get(i);

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

    private boolean validateProducts(
            StockTransferRequest request,
            Map<String, Product> productMap,
            BindingResult bindingResult
    ) {
        boolean hasError = false;

        for (int i = 0; i < request.items().size(); i++) {
            StockTransferItemRequest item = request.items().get(i);

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

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String generateTransferCode() {
        return "TRF" + LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        return value.trim();
    }
}
