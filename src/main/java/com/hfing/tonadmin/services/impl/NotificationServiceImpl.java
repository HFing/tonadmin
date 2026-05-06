package com.hfing.tonadmin.services.impl;

import com.hfing.tonadmin.common.NotificationChannel;
import com.hfing.tonadmin.common.NotificationType;
import com.hfing.tonadmin.dto.response.NotificationItem;
import com.hfing.tonadmin.dto.response.NotificationSummary;
import com.hfing.tonadmin.entities.*;
import com.hfing.tonadmin.repositories.NotificationReadRepository;
import com.hfing.tonadmin.repositories.NotificationRepository;
import com.hfing.tonadmin.services.CurrentUserService;
import com.hfing.tonadmin.services.NotificationService;
import com.hfing.tonadmin.services.NotificationStreamService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(VIETNAM_ZONE);
    private static final int DROPDOWN_LIMIT = 5;

    private final NotificationRepository notificationRepository;
    private final NotificationReadRepository notificationReadRepository;
    private final CurrentUserService currentUserService;
    private final NotificationStreamService notificationStreamService;

    @Override
    @Transactional(readOnly = true)
    public NotificationSummary getCurrentUserSummary() {
        User currentUser = currentUserService.getCurrentUser();
        String branchId = visibleBranchId();
        Instant since = Instant.now().minusSeconds(7 * 24 * 60 * 60);

        List<NotificationItem> messages = notificationRepository.findUnreadVisible(
                        NotificationChannel.MESSAGE,
                        branchId,
                        currentUser.getId(),
                        PageRequest.of(0, DROPDOWN_LIMIT)
                )
                .stream()
                .map(notification -> toItem(notification, false))
                .toList();

        List<NotificationItem> alerts = notificationRepository.findUnreadVisible(
                        NotificationChannel.ALERT,
                        branchId,
                        currentUser.getId(),
                        PageRequest.of(0, DROPDOWN_LIMIT)
                )
                .stream()
                .map(notification -> toItem(notification, false))
                .toList();

        return new NotificationSummary(
                notificationRepository.countRecentUnreadVisible(NotificationChannel.MESSAGE, branchId, currentUser.getId(), since),
                notificationRepository.countRecentUnreadVisible(NotificationChannel.ALERT, branchId, currentUser.getId(), since),
                messages,
                alerts
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationItem> getCurrentUserNotifications(NotificationChannel channel, Pageable pageable) {
        User currentUser = currentUserService.getCurrentUser();

        return notificationRepository.findVisible(channel, visibleBranchId(), pageable)
                .map(notification -> toItem(
                        notification,
                        notificationReadRepository.existsByNotificationIdAndUserId(notification.getId(), currentUser.getId())
                ));
    }

    @Override
    @Transactional
    public String markReadAndGetTargetUrl(String notificationId) {
        User currentUser = currentUserService.getCurrentUser();
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay thong bao"));

        if (!canCurrentUserSee(notification)) {
            throw new IllegalArgumentException("Ban khong co quyen xem thong bao nay");
        }

        if (!notificationReadRepository.existsByNotificationIdAndUserId(notificationId, currentUser.getId())) {
            notificationReadRepository.save(NotificationRead.builder()
                    .notification(notification)
                    .user(currentUser)
                    .readAt(Instant.now())
                    .build());
        }

        publishAfterCommit();

        return notification.getTargetUrl() == null || notification.getTargetUrl().isBlank()
                ? "/notifications?channel=" + notification.getChannel()
                : notification.getTargetUrl();
    }

    @Override
    @Transactional
    public void notifySalesOrderCreated(SalesOrder salesOrder) {
        Branch branch = salesOrder.getBranch();

        save(
                NotificationChannel.MESSAGE,
                NotificationType.SALES_ORDER_CREATED,
                "Đơn hàng mới",
                "Đơn " + salesOrder.getOrderCode() + " vừa được tạo tại " + branchName(branch) + ".",
                "/sales-orders/" + salesOrder.getId(),
                branch
        );
    }

    @Override
    @Transactional
    public void notifyPaymentRecorded(SalesOrder salesOrder) {
        Branch branch = salesOrder.getBranch();

        save(
                NotificationChannel.ALERT,
                NotificationType.PAYMENT_RECORDED,
                "Thanh toán mới",
                "Đơn " + salesOrder.getOrderCode() + " vừa ghi nhận thanh toán.",
                "/sales-orders/" + salesOrder.getId(),
                branch
        );
    }

    @Override
    @Transactional
    public void notifyStockStatusIfNeeded(Inventory inventory) {
        if (inventory == null || inventory.getProduct() == null || inventory.getBranch() == null) {
            return;
        }

        BigDecimal quantity = zeroIfNull(inventory.getQuantity());
        BigDecimal minStock = zeroIfNull(inventory.getProduct().getMinStock());

        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            save(
                    NotificationChannel.MESSAGE,
                    NotificationType.STOCK_OUT,
                    "Hàng đã hết",
                    inventory.getProduct().getCode() + " - " + inventory.getProduct().getName()
                            + " đã hết hàng tại " + branchName(inventory.getBranch()) + ".",
                    "/inventories?branchId=" + inventory.getBranch().getId()
                            + "&productId=" + inventory.getProduct().getId()
                            + "&stockStatus=OUT_OF_STOCK",
                    inventory.getBranch()
            );
            return;
        }

        if (quantity.compareTo(minStock) <= 0) {
            save(
                    NotificationChannel.MESSAGE,
                    NotificationType.STOCK_LOW,
                    "Hàng sắp hết",
                    inventory.getProduct().getCode() + " - " + inventory.getProduct().getName()
                            + " còn " + quantity.stripTrailingZeros().toPlainString()
                            + " tại " + branchName(inventory.getBranch()) + ".",
                    "/inventories?branchId=" + inventory.getBranch().getId()
                            + "&productId=" + inventory.getProduct().getId()
                            + "&stockStatus=LOW_STOCK",
                    inventory.getBranch()
            );
        }
    }

    private void save(
            NotificationChannel channel,
            NotificationType type,
            String title,
            String message,
            String targetUrl,
            Branch branch
    ) {
        notificationRepository.save(Notification.builder()
                .channel(channel)
                .type(type)
                .title(title)
                .message(message)
                .targetUrl(targetUrl)
                .branch(branch)
                .build());

        publishAfterCommit();
    }

    private void publishAfterCommit() {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            notificationStreamService.publish();
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                notificationStreamService.publish();
            }
        });
    }

    private NotificationItem toItem(Notification notification, boolean read) {
        return new NotificationItem(
                notification.getId(),
                notification.getChannel(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getTargetUrl(),
                notification.getCreatedAt() == null
                        ? ""
                        : DATE_TIME_FORMATTER.format(notification.getCreatedAt()),
                read
        );
    }

    private String visibleBranchId() {
        return currentUserService.isAdmin()
                ? null
                : currentUserService.getCurrentUserBranch().getId();
    }

    private boolean canCurrentUserSee(Notification notification) {
        if (currentUserService.isAdmin()) {
            return true;
        }

        return notification.getBranch() != null
                && notification.getBranch().getId().equals(currentUserService.getCurrentUserBranch().getId());
    }

    private String branchName(Branch branch) {
        return branch == null ? "chi nhánh" : branch.getName();
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
