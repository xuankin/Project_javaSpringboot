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
    public ResponseEntity<List<String>> getChatUsers() {
        return ResponseEntity.ok(chatService.getUserList());
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
}