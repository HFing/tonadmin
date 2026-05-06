package com.hfing.tonadmin.services;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface NotificationStreamService {

    SseEmitter subscribe(String userId);

    void publish();
}
