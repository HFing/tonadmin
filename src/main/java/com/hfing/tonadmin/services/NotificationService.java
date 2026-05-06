package com.hfing.tonadmin.services;

import com.hfing.tonadmin.dto.response.NotificationSummary;
import com.hfing.tonadmin.common.NotificationChannel;
import com.hfing.tonadmin.dto.response.NotificationItem;
import com.hfing.tonadmin.entities.Branch;
import com.hfing.tonadmin.entities.Inventory;
import com.hfing.tonadmin.entities.SalesOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationService {

    NotificationSummary getCurrentUserSummary();

    Page<NotificationItem> getCurrentUserNotifications(NotificationChannel channel, Pageable pageable);

    String markReadAndGetTargetUrl(String notificationId);

    void notifySalesOrderCreated(SalesOrder salesOrder);

    void notifyPaymentRecorded(SalesOrder salesOrder);

    void notifyStockStatusIfNeeded(Inventory inventory);
}
