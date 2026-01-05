package com.motorental.service;

import com.motorental.entity.ChatMessage;
import com.motorental.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {
    private final ChatMessageRepository chatMessageRepository;

    public ChatMessage saveMessage(com.motorental.dto.chat.ChatMessage dto, boolean isAdmin) {
        ChatMessage message = ChatMessage.builder()
                .sender(isAdmin ? "Admin" : dto.getSender())
                .receiver(isAdmin ? dto.getReceiver() : "Admin")
                .content(dto.getContent())
                .timestamp(LocalDateTime.now())
                .isAdminSender(isAdmin)
                .build();
        return chatMessageRepository.save(message);
    }

    public List<ChatMessage> getHistory(String username) {
        return chatMessageRepository.findChatHistoryByUser(username);
    }

    public List<String> getUserList() {
        return chatMessageRepository.findAllChatUsers();
    }
}