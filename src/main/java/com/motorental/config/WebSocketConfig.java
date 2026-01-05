package com.motorental.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Endpoint để client kết nối
        registry.addEndpoint("/ws").withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Prefix cho các tin nhắn từ Client gửi lên Server (@MessageMapping)
        registry.setApplicationDestinationPrefixes("/app");

        // --- SỬA ĐỔI QUAN TRỌNG ---
        // Thêm "/queue" để Broker chấp nhận các tin nhắn riêng tư (convertAndSendToUser)
        // Spring dịch destination "/user/..." thành "/queue/..." nội bộ
        registry.enableSimpleBroker("/topic", "/queue");

        // Prefix cho các tin nhắn gửi riêng cho từng User
        registry.setUserDestinationPrefix("/user");
    }
}