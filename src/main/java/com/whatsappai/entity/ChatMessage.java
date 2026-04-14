package com.whatsappai.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "chat_messages")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    @Column(name = "customer_phone", nullable = false)
    private String customerPhone;

    private String direction; // IN or OUT


    @Column(columnDefinition = "TEXT")
    private String content;

    @Builder.Default
    @Column(name = "message_type")
    private String messageType = "TEXT";

    @Column(name = "session_id")
    private String sessionId;

    @Builder.Default
    @Column(name = "guardrail_triggered")
    private Boolean guardrailTriggered = false;

    @Column(name = "guardrail_reason")
    private String guardrailReason;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
