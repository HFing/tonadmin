package com.hfing.tonadmin.services.impl;

import com.hfing.tonadmin.common.PaymentAction;
import com.hfing.tonadmin.common.PaymentMethod;
import com.hfing.tonadmin.common.PaymentStatus;
import com.hfing.tonadmin.common.SalesOrderStatus;
import com.hfing.tonadmin.common.StockTransactionType;
import com.hfing.tonadmin.dto.request.CancelSalesOrderRequest;
import com.hfing.tonadmin.dto.request.PaymentUpdateRequest;
import com.hfing.tonadmin.dto.request.SalesOrderItemRequest;
import com.hfing.tonadmin.dto.request.SalesOrderRequest;
import com.hfing.tonadmin.dto.request.SalesOrderSearchRequest;
import com.hfing.tonadmin.entities.*;
import com.hfing.tonadmin.repositories.*;
import com.hfing.tonadmin.services.CurrentUserService;
import com.hfing.tonadmin.services.NotificationService;
import com.hfing.tonadmin.services.SalesOrderService;
import com.hfing.tonadmin.specifications.SalesOrderSpecifications;
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
public class SalesOrderServiceImpl implements SalesOrderService {

    private final SalesOrderRepository salesOrderRepository;
    private final SalesOrderItemRepository salesOrderItemRepository;
    private final SalesPaymentRepository salesPaymentRepository;
    private final BranchRepository branchRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final StockTransactionRepository stockTransactionRepository;
    private final CurrentUserService currentUserService;
    private final NotificationService notificationService;

    @Override
    public Page<SalesOrder> getSalesOrders(SalesOrderSearchRequest search, Pageable pageable) {
        SalesOrderSearchRequest scopedSearch = scopeSearchToCurrentUser(search);

        return salesOrderRepository.findAll(SalesOrderSpecifications.bySearch(scopedSearch), pageable);
    }

    @Override
    public SalesOrder getSalesOrderById(String id) {
        if (currentUserService.isAdmin()) {
            return salesOrderRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn bán hàng"));
        }

        Branch branch = currentUserService.getCurrentUserBranch();

        return salesOrderRepository.findByIdAndBranchId(id, branch.getId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn bán hàng hoặc bạn không có quyền xem"));
    }

    @Override
    public List<SalesOrderItem> getSalesOrderItems(String salesOrderId) {
        return salesOrderItemRepository.findBySalesOrderId(salesOrderId);
    }

    @Override
    public List<SalesPayment> getSalesPayments(String salesOrderId) {
        return salesPaymentRepository.findBySalesOrderIdOrderByCreatedAtDesc(salesOrderId);
    }

    @Override
    @Transactional
    public SalesOrder createSalesOrder(SalesOrderRequest request, BindingResult bindingResult) {
        Branch branch;

        if (currentUserService.isAdmin()) {
            branch = branchRepository.findById(request.branchId()).orElse(null);
        } else {
            branch = currentUserService.getCurrentUserBranch();
        }

        if (branch == null || !Boolean.TRUE.equals(branch.getActive())) {
            bindingResult.rejectValue("branchId", "invalid", "Chi nhánh không hợp lệ hoặc đã bị khóa");
            return null;
        }

        if (request.items() == null || request.items().isEmpty()) {
            bindingResult.rejectValue("items", "required", "Vui lòng thêm ít nhất một sản phẩm");
            return null;
        }

        boolean hasError = validateOrderItems(request, bindingResult);
        if (hasError) {
            return null;
        }

        List<String> productIds = request.items()
                .stream()
                .map(SalesOrderItemRequest::productId)
                .toList();

        Map<String, Product> productMap = productRepository.findAllById(productIds)
                .stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        hasError = validateProducts(request, productMap, bindingResult);
        if (hasError) {
            return null;
        }

        String orderCode = generateOrderCode();
        String batchCode = "SALE" + orderCode;
        String orderNote = trimToNull(request.note());

        SalesOrder salesOrder = SalesOrder.builder()
                .orderCode(orderCode)
                .branch(branch)
                .customerName(trimToNull(request.customerName()))
                .customerPhone(trimToNull(request.customerPhone()))
                .note(orderNote)
                .status(SalesOrderStatus.COMPLETED)
                .totalAmount(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .finalAmount(BigDecimal.ZERO)
                .paidAmount(BigDecimal.ZERO)
                .refundedAmount(BigDecimal.ZERO)
                .remainingAmount(BigDecimal.ZERO)
                .paymentStatus(PaymentStatus.UNPAID)
                .paymentMethod(null)
                .build();

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (int i = 0; i < request.items().size(); i++) {
            SalesOrderItemRequest itemRequest = request.items().get(i);
            Product product = productMap.get(itemRequest.productId());

            Inventory inventory = inventoryRepository.findWithLockByBranchAndProduct(branch, product)
                    .orElse(null);

            if (inventory == null) {
                bindingResult.rejectValue(
                        "items[" + i + "].productId",
                        "noStock",
                        "Sản phẩm chưa có tồn kho tại chi nhánh này"
                );
                return null;
            }

            BigDecimal beforeQuantity = inventory.getQuantity() == null
                    ? BigDecimal.ZERO
                    : inventory.getQuantity();

            BigDecimal saleQuantity = itemRequest.quantity();

            if (beforeQuantity.compareTo(saleQuantity) < 0) {
                bindingResult.rejectValue(
                        "items[" + i + "].quantity",
                        "insufficientStock",
                        "Tồn kho không đủ. Hiện còn: " + beforeQuantity
                );
                return null;
            }

            BigDecimal afterQuantity = beforeQuantity.subtract(saleQuantity);
            inventory.setQuantity(afterQuantity);
            inventoryRepository.save(inventory);
            notificationService.notifyStockStatusIfNeeded(inventory);

            BigDecimal sellingPrice = product.getSellingPrice() == null
                    ? BigDecimal.ZERO
                    : product.getSellingPrice();

            BigDecimal lineTotal = sellingPrice.multiply(saleQuantity);
            totalAmount = totalAmount.add(lineTotal);

            SalesOrderItem salesOrderItem = SalesOrderItem.builder()
                    .salesOrder(salesOrder)
                    .product(product)
                    .quantity(saleQuantity)
                    .sellingPrice(sellingPrice)
                    .totalPrice(lineTotal)
                    .build();

            salesOrder.getItems().add(salesOrderItem);

            StockTransaction transaction = StockTransaction.builder()
                    .batchCode(batchCode)
                    .batchNote(orderNote)
                    .branch(branch)
                    .product(product)
                    .transactionType(StockTransactionType.SALE)
                    .quantity(saleQuantity)
                    .beforeQuantity(beforeQuantity)
                    .afterQuantity(afterQuantity)
                    .note(orderNote)
                    .build();

            stockTransactionRepository.save(transaction);
        }

        BigDecimal discountAmount = normalizeDiscountAmount(request.discountAmount(), totalAmount);
        BigDecimal finalAmount = totalAmount.subtract(discountAmount);

        salesOrder.setTotalAmount(totalAmount);
        salesOrder.setDiscountAmount(discountAmount);
        salesOrder.setFinalAmount(finalAmount);
        salesOrder.setPaidAmount(BigDecimal.ZERO);
        salesOrder.setRefundedAmount(BigDecimal.ZERO);
        salesOrder.setRemainingAmount(finalAmount);
        salesOrder.setPaymentStatus(finalAmount.compareTo(BigDecimal.ZERO) == 0
                ? PaymentStatus.PAID
                : PaymentStatus.UNPAID
        );

        SalesOrder savedOrder = salesOrderRepository.save(salesOrder);
        notificationService.notifySalesOrderCreated(savedOrder);

        return savedOrder;
    }

    @Override
    @Transactional
    public boolean updatePayment(String salesOrderId, PaymentUpdateRequest request, BindingResult bindingResult) {
        SalesOrder salesOrder;

        try {
            salesOrder = getSalesOrderById(salesOrderId);
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("notFound", ex.getMessage());
            return false;
        }

        if (salesOrder == null) {
            bindingResult.reject("notFound", "Không tìm thấy đơn bán hàng");
            return false;
        }

        if (salesOrder.getStatus() == SalesOrderStatus.CANCELLED) {
            bindingResult.reject("cancelled", "Không thể thanh toán đơn hàng đã hủy");
            return false;
        }

        if (salesOrder.getPaymentStatus() == PaymentStatus.PAID) {
            bindingResult.reject("paid", "Đơn hàng đã thanh toán đủ");
            return false;
        }

        if (request.paymentMethod() == null) {
            bindingResult.rejectValue("paymentMethod", "required", "Vui lòng chọn phương thức thanh toán");
            return false;
        }

        BigDecimal amount = request.amount();

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            bindingResult.rejectValue("amount", "invalid", "Số tiền thanh toán phải lớn hơn 0");
            return false;
        }

        BigDecimal currentPaidAmount = zeroIfNull(salesOrder.getPaidAmount());
        BigDecimal finalAmount = zeroIfNull(salesOrder.getFinalAmount());
        BigDecimal remainingBeforePayment = finalAmount.subtract(currentPaidAmount);

        if (amount.compareTo(remainingBeforePayment) > 0) {
            bindingResult.rejectValue(
                    "amount",
                    "exceed",
                    "Số tiền thanh toán không được lớn hơn số tiền còn lại"
            );
            return false;
        }

        BigDecimal newPaidAmount = currentPaidAmount.add(amount);
        BigDecimal remainingAmount = finalAmount.subtract(newPaidAmount);

        salesOrder.setPaidAmount(newPaidAmount);
        salesOrder.setRemainingAmount(remainingAmount);
        salesOrder.setPaymentMethod(request.paymentMethod());
        salesOrder.setPaymentStatus(calculatePaymentStatus(newPaidAmount, finalAmount));

        salesPaymentRepository.save(SalesPayment.builder()
                .salesOrder(salesOrder)
                .paymentAction(PaymentAction.PAYMENT)
                .paymentMethod(request.paymentMethod())
                .amount(amount)
                .note("Thanh toán đơn " + salesOrder.getOrderCode())
                .build());

        salesOrderRepository.save(salesOrder);
        notificationService.notifyPaymentRecorded(salesOrder);

        return true;
    }

    @Override
    @Transactional
    public boolean cancelSalesOrder(
            String salesOrderId,
            CancelSalesOrderRequest request,
            BindingResult bindingResult
    ) {
        SalesOrder salesOrder;

        try {
            salesOrder = getSalesOrderById(salesOrderId);
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("notFound", ex.getMessage());
            return false;
        }

        if (salesOrder == null) {
            bindingResult.reject("notFound", "Không tìm thấy đơn bán hàng");
            return false;
        }

        if (salesOrder.getStatus() == SalesOrderStatus.CANCELLED) {
            bindingResult.reject("cancelled", "Đơn hàng đã bị hủy trước đó");
            return false;
        }

        List<SalesOrderItem> items = salesOrderItemRepository.findBySalesOrderId(salesOrderId);

        if (items.isEmpty()) {
            bindingResult.reject("emptyItems", "Đơn hàng không có sản phẩm để hoàn kho");
            return false;
        }

        String cancelReason = trimToNull(request.reason());
        String batchCode = "CANCEL" + salesOrder.getOrderCode();

        for (SalesOrderItem item : items) {
            Product product = item.getProduct();

            Inventory inventory = inventoryRepository.findWithLockByBranchAndProduct(
                    salesOrder.getBranch(),
                    product
            ).orElseGet(() -> Inventory.builder()
                    .branch(salesOrder.getBranch())
                    .product(product)
                    .quantity(BigDecimal.ZERO)
                    .build());

            BigDecimal beforeQuantity = zeroIfNull(inventory.getQuantity());
            BigDecimal returnQuantity = zeroIfNull(item.getQuantity());
            BigDecimal afterQuantity = beforeQuantity.add(returnQuantity);

            inventory.setQuantity(afterQuantity);
            inventoryRepository.save(inventory);

            stockTransactionRepository.save(StockTransaction.builder()
                    .batchCode(batchCode)
                    .batchNote(cancelReason)
                    .branch(salesOrder.getBranch())
                    .product(product)
                    .transactionType(StockTransactionType.RETURN)
                    .quantity(returnQuantity)
                    .beforeQuantity(beforeQuantity)
                    .afterQuantity(afterQuantity)
                    .note(cancelReason)
                    .build());
        }

        BigDecimal paidAmount = zeroIfNull(salesOrder.getPaidAmount());

        if (paidAmount.compareTo(BigDecimal.ZERO) > 0) {
            PaymentMethod refundMethod = salesOrder.getPaymentMethod() == null
                    ? PaymentMethod.OTHER
                    : salesOrder.getPaymentMethod();

            salesPaymentRepository.save(SalesPayment.builder()
                    .salesOrder(salesOrder)
                    .paymentAction(PaymentAction.REFUND)
                    .paymentMethod(refundMethod)
                    .amount(paidAmount)
                    .note("Hoàn tiền khi hủy đơn " + salesOrder.getOrderCode())
                    .build());

            salesOrder.setRefundedAmount(paidAmount);
            salesOrder.setPaymentStatus(PaymentStatus.REFUNDED);
        }

        salesOrder.setStatus(SalesOrderStatus.CANCELLED);
        salesOrder.setCancelReason(cancelReason);
        salesOrder.setRemainingAmount(BigDecimal.ZERO);

        salesOrderRepository.save(salesOrder);

        return true;
    }

    private SalesOrderSearchRequest scopeSearchToCurrentUser(SalesOrderSearchRequest search) {
        SalesOrderSearchRequest safeSearch = search == null
                ? new SalesOrderSearchRequest(null, null, null, null, null, null, null)
                : search;

        if (currentUserService.isAdmin()) {
            return safeSearch;
        }

        Branch branch = currentUserService.getCurrentUserBranch();

        return new SalesOrderSearchRequest(
                safeSearch.keyword(),
                branch.getId(),
                safeSearch.status(),
                safeSearch.paymentStatus(),
                safeSearch.debtOnly(),
                safeSearch.fromDate(),
                safeSearch.toDate()
        );
    }

    private boolean validateOrderItems(SalesOrderRequest request, BindingResult bindingResult) {
        boolean hasError = false;
        Set<String> productIds = new HashSet<>();

        for (int i = 0; i < request.items().size(); i++) {
            SalesOrderItemRequest item = request.items().get(i);

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
            SalesOrderRequest request,
            Map<String, Product> productMap,
            BindingResult bindingResult
    ) {
        boolean hasError = false;

        for (int i = 0; i < request.items().size(); i++) {
            SalesOrderItemRequest item = request.items().get(i);

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

    private BigDecimal normalizeDiscountAmount(BigDecimal discountAmount, BigDecimal totalAmount) {
        if (discountAmount == null || discountAmount.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }

        if (discountAmount.compareTo(totalAmount) > 0) {
            return totalAmount;
        }

        return discountAmount;
    }

    private PaymentStatus calculatePaymentStatus(BigDecimal paidAmount, BigDecimal finalAmount) {
        if (finalAmount.compareTo(BigDecimal.ZERO) == 0) {
            return PaymentStatus.PAID;
        }

        if (paidAmount.compareTo(BigDecimal.ZERO) == 0) {
            return PaymentStatus.UNPAID;
        }

        if (paidAmount.compareTo(finalAmount) < 0) {
            return PaymentStatus.PARTIALLY_PAID;
        }

        return PaymentStatus.PAID;
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String generateOrderCode() {
        return "SO" + LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        return value.trim();
    }
}
