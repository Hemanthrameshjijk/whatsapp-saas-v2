package com.whatsappai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationSession {
    private String customerName;

    @Builder.Default
    private ConvStage stage = ConvStage.GREETING;

    @Builder.Default
    private List<SessionMessage> messages = new ArrayList<>();

    @Builder.Default
    private Map<String, CartItem> cart = new HashMap<>(); // key = productId.toString()

    @Builder.Default
    private int messageCount = 0;

    @Builder.Default
    private int failCount = 0;

    // Delivery info captured from LOCATION message
    private String deliveryAddress;
    private Double deliveryLat;
    private Double deliveryLng;

    public void addMessage(SessionMessage msg) {
        messages.add(msg);
        if (messages.size() > 20) messages.remove(0);
        messageCount++;
    }

    public void incrementMessageCount() {
        messageCount++;
    }

    public List<SessionMessage> getLast(int n) {
        int size = messages.size();
        return messages.subList(Math.max(0, size - n), size);
    }
}
