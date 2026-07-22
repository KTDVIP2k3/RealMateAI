package com.GSU26SE22_SU26SE002.RealMateAI.config;

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
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // /topic/** — server bắn thông báo xuống client (broadcast theo topic).
        registry.enableSimpleBroker("/topic");
        // /app/** — dành cho client gửi lên server (không dùng ở bản test này,
        // khai báo sẵn cho đầy đủ quy ước STOMP, không bắt buộc phải có endpoint xử lý).
        registry.setApplicationDestinationPrefixes("/app");
    }
}
