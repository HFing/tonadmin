package com.hfing.tonadmin.dto.response;

import com.hfing.tonadmin.common.NotificationChannel;
import com.hfing.tonadmin.common.NotificationType;

public record NotificationItem(
        String id,
        NotificationChannel channel,
        NotificationType type,
        String title,
        String message,
        String targetUrl,
        String createdAt,
        boolean read
) {
}
