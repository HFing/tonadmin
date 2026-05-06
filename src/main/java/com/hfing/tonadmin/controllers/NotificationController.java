package com.hfing.tonadmin.controllers;

import com.hfing.tonadmin.common.NotificationChannel;
import com.hfing.tonadmin.dto.response.NotificationSummary;
import com.hfing.tonadmin.services.CurrentUserService;
import com.hfing.tonadmin.services.NotificationService;
import com.hfing.tonadmin.services.NotificationStreamService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Controller
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_STAFF')")
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationStreamService notificationStreamService;
    private final CurrentUserService currentUserService;

    @GetMapping("/notifications")
    public String index(
            @RequestParam(defaultValue = "MESSAGE") NotificationChannel channel,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        model.addAttribute("channel", channel);
        model.addAttribute("notificationPage", notificationService.getCurrentUserNotifications(channel, pageable));
        model.addAttribute("channels", NotificationChannel.values());

        return "notifications/index";
    }

    @GetMapping("/notifications/{id}/open")
    public RedirectView open(@PathVariable String id) {
        return new RedirectView(notificationService.markReadAndGetTargetUrl(id));
    }

    @GetMapping("/notifications/summary")
    @org.springframework.web.bind.annotation.ResponseBody
    public NotificationSummary summary() {
        return notificationService.getCurrentUserSummary();
    }

    @GetMapping(path = "/notifications/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @org.springframework.web.bind.annotation.ResponseBody
    public SseEmitter stream() {
        return notificationStreamService.subscribe(currentUserService.getCurrentUser().getId());
    }
}
