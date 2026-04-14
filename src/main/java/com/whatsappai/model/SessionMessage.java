package com.whatsappai.model;

import com.whatsappai.entity.ChatMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionMessage {
    private String role; // "user" or "assistant"
    private String content;
    private LocalDateTime timestamp;

    public static SessionMessage userMessage(String content) {
        return SessionMessage.builder()
            .role("user").content(content).timestamp(LocalDateTime.now()).build();
    }

    public static SessionMessage assistantMessage(String content) {
        return SessionMessage.builder()
            .role("assistant").content(content).timestamp(LocalDateTime.now()).build();
    }

    public static SessionMessage from(ChatMessage msg) {
        String role = "IN".equals(msg.getDirection()) ? "user" : "assistant";
        return SessionMessage.builder()
            .role(role).content(msg.getContent()).timestamp(msg.getCreatedAt()).build();
    }
}
