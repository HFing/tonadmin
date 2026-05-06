package com.hfing.tonadmin.repositories;

import com.hfing.tonadmin.entities.NotificationRead;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationReadRepository extends JpaRepository<NotificationRead, String> {

    boolean existsByNotificationIdAndUserId(String notificationId, String userId);
}
