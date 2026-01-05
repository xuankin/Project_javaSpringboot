package com.motorental.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChatMessage {
    private String sender;    // Người gửi
    private String receiver;  // Người nhận (dùng khi admin reply)
    private String content;   // Nội dung
    private MessageType type;

    public enum MessageType {
        CHAT, JOIN, LEAVE
    }
}