package com.motorental.repository;

import com.motorental.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // Lấy lịch sử chat giữa Admin và một User cụ thể, sắp xếp theo thời gian
    // (Lấy tin nhắn A gửi B hoặc B gửi A)
    @Query("SELECT m FROM ChatMessage m WHERE (m.sender = :user OR m.receiver = :user) ORDER BY m.timestamp ASC")
    List<ChatMessage> findChatHistoryByUser(String user);

    // Lấy danh sách những user đã từng nhắn tin (để hiển thị bên sidebar Admin)
    @Query("SELECT DISTINCT m.sender FROM ChatMessage m WHERE m.sender != 'Admin'")
    List<String> findAllChatUsers();
}