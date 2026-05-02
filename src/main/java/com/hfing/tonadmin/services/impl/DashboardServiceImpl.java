package com.hfing.tonadmin.services.impl;

import com.hfing.tonadmin.common.PaymentStatus;
import com.hfing.tonadmin.common.SalesOrderStatus;
import com.hfing.tonadmin.dto.response.DashboardStats;
import com.hfing.tonadmin.dto.response.LowStockItem;
import com.hfing.tonadmin.dto.response.RecentOrderItem;
import com.hfing.tonadmin.dto.response.RecentStockActivityItem;
import com.hfing.tonadmin.entities.Branch;
import com.hfing.tonadmin.entities.Inventory;
import com.hfing.tonadmin.entities.SalesOrder;
import com.hfing.tonadmin.entities.StockTransaction;
import com.hfing.tonadmin.repositories.InventoryRepository;
import com.hfing.tonadmin.repositories.SalesOrderRepository;
import com.hfing.tonadmin.repositories.StockTransactionRepository;
import com.hfing.tonadmin.services.CurrentUserService;
import com.hfing.tonadmin.services.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(VIETNAM_ZONE);

    private final SalesOrderRepository salesOrderRepository;
    private final InventoryRepository inventoryRepository;
    private final StockTransactionRepository stockTransactionRepository;
    private final CurrentUserService currentUserService;

    @Override
    public DashboardStats getDashboardStats(String branchId) {
        LocalDate today = LocalDate.now(VIETNAM_ZONE);

        Instant startOfToday = today.atStartOfDay(VIETNAM_ZONE).toInstant();
        Instant startOfTomorrow = today.plusDays(1).atStartOfDay(VIETNAM_ZONE).toInstant();

        Instant startOfMonth = today.withDayOfMonth(1).atStartOfDay(VIETNAM_ZONE).toInstant();
        Instant startOfNextMonth = today.plusMonths(1).withDayOfMonth(1).atStartOfDay(VIETNAM_ZONE).toInstant();

        if (!currentUserService.isAdmin()) {
            Branch staffBranch = currentUserService.getCurrentUserBranch();

            return buildBranchStats(
                    staffBranch.getId(),
                    startOfToday,
                    startOfTomorrow,
                    startOfMonth,
                    startOfNextMonth
            );
        }

        if (branchId != null && !branchId.isBlank()) {
            return buildBranchStats(
                    branchId,
                    startOfToday,
                    startOfTomorrow,
                    startOfMonth,
                    startOfNextMonth
            );
        }

        return buildAdminStats(
                startOfToday,
                startOfTomorrow,
                startOfMonth,
                startOfNextMonth
        );
    }

    private DashboardStats buildAdminStats(
            Instant startOfToday,
            Instant startOfTomorrow,
            Instant startOfMonth,
            Instant startOfNextMonth
    ) {
        BigDecimal revenueToday = salesOrderRepository.sumRevenue(
                startOfToday,
                startOfTomorrow,
                SalesOrderStatus.CANCELLED
        );

        BigDecimal revenueThisMonth = salesOrderRepository.sumRevenue(
                startOfMonth,
                startOfNextMonth,
                SalesOrderStatus.CANCELLED
        );

        long ordersToday = salesOrderRepository.countOrders(
                startOfToday,
                startOfTomorrow,
                SalesOrderStatus.CANCELLED
        );

        long unpaidOrders = salesOrderRepository.countUnpaidOrders(
                SalesOrderStatus.CANCELLED,
                PaymentStatus.PAID
        );

        BigDecimal unpaidAmount = salesOrderRepository.sumUnpaidAmount(
                SalesOrderStatus.CANCELLED,
                PaymentStatus.PAID
        );

        long lowStockCount = inventoryRepository.countLowStock();
        long outOfStockCount = inventoryRepository.countOutOfStock();

        List<RecentOrderItem> recentOrders = salesOrderRepository.findTop5ByOrderByCreatedAtDesc()
                .stream()
                .map(this::toRecentOrderItem)
                .toList();

        List<LowStockItem> lowStockItems = inventoryRepository.findTopLowStock(PageRequest.of(0, 5))
                .stream()
                .map(this::toLowStockItem)
                .toList();

        List<RecentStockActivityItem> recentStockActivities = stockTransactionRepository.findTop5ByOrderByCreatedAtDesc()
                .stream()
                .map(this::toRecentStockActivityItem)
                .toList();

        return new DashboardStats(
                revenueToday,
                revenueThisMonth,
                ordersToday,
                unpaidOrders,
                unpaidAmount,
                lowStockCount,
                outOfStockCount,
                recentOrders,
                lowStockItems,
                recentStockActivities
        );
    }

    private DashboardStats buildBranchStats(
            String branchId,
            Instant startOfToday,
            Instant startOfTomorrow,
            Instant startOfMonth,
            Instant startOfNextMonth
    ) {
        BigDecimal revenueToday = salesOrderRepository.sumRevenueByBranch(
                branchId,
                startOfToday,
                startOfTomorrow,
                SalesOrderStatus.CANCELLED
        );

        BigDecimal revenueThisMonth = salesOrderRepository.sumRevenueByBranch(
                branchId,
                startOfMonth,
                startOfNextMonth,
                SalesOrderStatus.CANCELLED
        );

        long ordersToday = salesOrderRepository.countOrdersByBranch(
                branchId,
                startOfToday,
                startOfTomorrow,
                SalesOrderStatus.CANCELLED
        );

        long unpaidOrders = salesOrderRepository.countUnpaidOrdersByBranch(
                branchId,
                SalesOrderStatus.CANCELLED,
                PaymentStatus.PAID
        );

        BigDecimal unpaidAmount = salesOrderRepository.sumUnpaidAmountByBranch(
                branchId,
                SalesOrderStatus.CANCELLED,
                PaymentStatus.PAID
        );

        long lowStockCount = inventoryRepository.countLowStockByBranch(branchId);
        long outOfStockCount = inventoryRepository.countOutOfStockByBranch(branchId);

        List<RecentOrderItem> recentOrders = salesOrderRepository.findTop5ByBranchIdOrderByCreatedAtDesc(branchId)
                .stream()
                .map(this::toRecentOrderItem)
                .toList();

        List<LowStockItem> lowStockItems = inventoryRepository.findTopLowStockByBranch(branchId, PageRequest.of(0, 5))
                .stream()
                .map(this::toLowStockItem)
                .toList();

        List<RecentStockActivityItem> recentStockActivities = stockTransactionRepository.findTop5ByBranchIdOrderByCreatedAtDesc(branchId)
                .stream()
                .map(this::toRecentStockActivityItem)
                .toList();

        return new DashboardStats(
                revenueToday,
                revenueThisMonth,
                ordersToday,
                unpaidOrders,
                unpaidAmount,
                lowStockCount,
                outOfStockCount,
                recentOrders,
                lowStockItems,
                recentStockActivities
        );
    }

    private RecentOrderItem toRecentOrderItem(SalesOrder order) {
        return new RecentOrderItem(
                order.getId(),
                order.getOrderCode(),
                order.getCustomerName() == null || order.getCustomerName().isBlank()
                        ? "Khách lẻ"
                        : order.getCustomerName(),
                order.getBranch() == null ? "—" : order.getBranch().getName(),
                zeroIfNull(order.getFinalAmount()),
                zeroIfNull(order.getRemainingAmount()),
                order.getPaymentStatus() == null ? "—" : order.getPaymentStatus().name(),
                order.getCreatedAt() == null ? "—" : DATE_TIME_FORMATTER.format(order.getCreatedAt())
        );
    }

    private LowStockItem toLowStockItem(Inventory inventory) {
        return new LowStockItem(
                inventory.getBranch() == null ? "—" : inventory.getBranch().getName(),
                inventory.getProduct() == null ? "—" : inventory.getProduct().getCode(),
                inventory.getProduct() == null ? "—" : inventory.getProduct().getName(),
                zeroIfNull(inventory.getQuantity()),
                inventory.getProduct() == null ? BigDecimal.ZERO : zeroIfNull(inventory.getProduct().getMinStock())
        );
    }

    private RecentStockActivityItem toRecentStockActivityItem(StockTransaction transaction) {
        return new RecentStockActivityItem(
                transaction.getBranch() == null ? "—" : transaction.getBranch().getName(),
                transaction.getProduct() == null ? "—" : transaction.getProduct().getCode(),
                transaction.getProduct() == null ? "—" : transaction.getProduct().getName(),
                transaction.getTransactionType() == null ? "—" : transaction.getTransactionType().name(),
                zeroIfNull(transaction.getQuantity()),
                transaction.getCreatedAt() == null ? "—" : DATE_TIME_FORMATTER.format(transaction.getCreatedAt())
        );
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}