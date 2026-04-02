package com.motorental.controller;

import com.motorental.dto.chat.ChatMessage;
import com.motorental.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.security.Principal;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;

    // --- WEBSOCKET HANDLERS ---
    @MessageMapping("/chat.sendMessage")
    @SendTo("/topic/admin")
    public ChatMessage sendMessage(@Payload ChatMessage chatMessage, Principal principal) {
        if(principal != null) {
            chatMessage.setSender(principal.getName());
        }
        chatService.saveMessage(chatMessage, false);
        return chatMessage;
    }

    @MessageMapping("/chat.reply")
    public void replyToUser(@Payload ChatMessage chatMessage) {
        chatService.saveMessage(chatMessage, true);
        messagingTemplate.convertAndSendToUser(
                chatMessage.getReceiver(),
                "/queue/reply",
                chatMessage
        );
    }

    // --- PAGES ---

    // 1. Trang chat cho User (ĐÃ SỬA: Thêm /user vào đường dẫn)
    @GetMapping("/user/chat")
    public String userChatPage() {
        return "user/chat";
    }

    // 2. Trang chat cho Admin
    @GetMapping("/admin/chat")
    public String adminChatPage() {
        return "admin/chat/index";
    }

    // --- API ---
    @GetMapping("/api/chat/users")
    @ResponseBody
    public ResponseEntity<List<java.util.Map<String, Object>>> getChatUsers() {
        List<String> users = chatService.getUserList();
        List<java.util.Map<String, Object>> result = new java.util.ArrayList<>();
        for (String user : users) {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("username", user);
            map.put("unreadCount", chatService.getUnreadCountFromUserToAdmin(user));
            result.add(map);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/api/chat/history")
    @ResponseBody
    public ResponseEntity<List<com.motorental.entity.ChatMessage>> getChatHistory(@RequestParam("user") String user) {
        return ResponseEntity.ok(chatService.getHistory(user));
    }

    @GetMapping("/api/chat/my-history")
    @ResponseBody
    public ResponseEntity<List<com.motorental.entity.ChatMessage>> getMyChatHistory(Principal principal) {
        if (principal == null) return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(chatService.getHistory(principal.getName()));
    }

    @GetMapping("/api/chat/unread-count")
    @ResponseBody
    public ResponseEntity<Long> getUnreadCount(Principal principal) {
        if (principal == null) return ResponseEntity.ok(0L);
        return ResponseEntity.ok(chatService.getUnreadCount(principal.getName()));
    }

    @GetMapping("/api/chat/admin/unread-count")
    @ResponseBody
    public ResponseEntity<Long> getAdminUnreadCount(@RequestParam(value = "user", required = false) String user) {
        if (user != null) {
            return ResponseEntity.ok(chatService.getUnreadCountFromUserToAdmin(user));
        }
        return ResponseEntity.ok(chatService.getUnreadCount("Admin"));
    }

    @PostMapping("/api/chat/mark-read")
    @ResponseBody
    public ResponseEntity<Void> markAsRead(@RequestParam("sender") String sender, Principal principal) {
        if (principal == null) return ResponseEntity.badRequest().build();
        // Nếu client là Admin, nó sẽ gửi principal tên là tài khoản admin, nhưng receiver trong entity gọi là Admin.
        // Để dễ, ta tách endpoint Admin và User.
        chatService.markAsRead(principal.getName(), sender);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/api/chat/admin/mark-read")
    @ResponseBody
    public ResponseEntity<Void> adminMarkAsRead(@RequestParam("sender") String sender) {
        chatService.markAsRead("Admin", sender);
        return ResponseEntity.ok().build();
    }
}