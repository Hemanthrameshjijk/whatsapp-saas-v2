package com.whatsappai.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelRequest {
    private String model;
    private String systemPrompt;

    @Builder.Default
    private List<Message> conversationHistory = new ArrayList<>();

    @Builder.Default
    private List<ToolDefinition> tools = new ArrayList<>();

    @Builder.Default
    private double temperature = 0.4;

    @Builder.Default
    private int maxTokens = 512;
}
