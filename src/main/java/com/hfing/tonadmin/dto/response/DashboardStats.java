package com.hfing.tonadmin.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record DashboardStats(
        BigDecimal revenueToday,
        BigDecimal revenueThisMonth,
        long ordersToday,
        long unpaidOrders,
        BigDecimal unpaidAmount,
        long lowStockCount,
        long outOfStockCount,
        List<RecentOrderItem> recentOrders,
        List<LowStockItem> lowStockItems,
        List<RecentStockActivityItem> recentStockActivities
) {
}