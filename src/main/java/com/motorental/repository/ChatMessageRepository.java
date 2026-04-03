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

    // Lấy danh sách những user đã từng nhắn tin, sắp xếp theo thời gian mới nhất
    @Query("SELECT m.sender FROM ChatMessage m WHERE m.sender != 'Admin' GROUP BY m.sender ORDER BY MAX(m.timestamp) DESC")
    List<String> findAllChatUsers();

    // Đếm số tin nhắn chưa đọc của một người nhận cụ thể
    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.receiver = :receiver AND m.isRead = false")
    long countUnreadMessagesByReceiver(String receiver);

    // Đếm số tin nhắn chưa đọc từ một người gửi cụ thể đến Admin (dùng cho sidebar Admin)
    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.receiver = 'Admin' AND m.sender = :user AND m.isRead = false")
    long countUnreadFromUserToAdmin(String user);

    // Đánh dấu đã đọc
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query("UPDATE ChatMessage m SET m.isRead = true WHERE m.receiver = :receiver AND m.sender = :sender")
    void markMessagesAsRead(String receiver, String sender);
}