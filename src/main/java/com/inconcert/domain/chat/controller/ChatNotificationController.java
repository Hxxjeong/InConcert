package com.inconcert.domain.chat.controller;

import com.inconcert.domain.chat.dto.NotificationMessage;
import com.inconcert.domain.chat.entity.ChatNotification;
import com.inconcert.domain.chat.service.ChatNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class ChatNotificationController {
    private final ChatNotificationService chatNotificationService;

    @GetMapping("/requestlist")
    public List<NotificationMessage> getRequestList(@RequestParam("userId") Long userId) {
        List<ChatNotification> notifications = chatNotificationService.findByUserId(userId);
        return notifications.stream()
                .map(NotificationMessage::new)
                .collect(Collectors.toList());
    }
}
