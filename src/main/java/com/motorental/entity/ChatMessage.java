package com.motorental.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sender;    // Người gửi (username)
    private String receiver;  // Người nhận (User hoặc Admin)

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String content;   // Nội dung

    @Column(columnDefinition = "NVARCHAR(500)")
    private String imageUrl;  // URL ảnh đính kèm

    @Column(length = 20)
    private String messageType; // TEXT, IMAGE, SYSTEM

    private LocalDateTime timestamp; // Thời gian gửi

    // Đánh dấu tin nhắn là của ai để dễ query
    // true: Admin gửi, false: User gửi
    // Đánh dấu tin nhắn là của admin
    private boolean isAdminSender;

    // Đánh dấu tin nhắn đã đọc chưa (false = chưa đọc)
    // Admin chưa đọc = user gửi lên mà admin chưa xem
    @Builder.Default
    private boolean isRead = false;
}