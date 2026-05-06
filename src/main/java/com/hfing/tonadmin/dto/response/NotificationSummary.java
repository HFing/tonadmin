package com.hfing.tonadmin.dto.response;

import java.util.List;

public record NotificationSummary(
        long messageCount,
        long alertCount,
        List<NotificationItem> messages,
        List<NotificationItem> alerts
) {
}
