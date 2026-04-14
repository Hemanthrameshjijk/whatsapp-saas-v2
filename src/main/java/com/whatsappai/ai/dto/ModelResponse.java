package com.whatsappai.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelResponse {
    private String content;
    private boolean hasToolCall;
    private String toolName;
    private Map<String, Object> toolArguments;
    private String toolCallId;
    private boolean timedOut;

    public static ModelResponse text(String content) {
        return ModelResponse.builder().content(content).hasToolCall(false).build();
    }

    public static ModelResponse toolCall(String toolCallId, String toolName, Map<String, Object> args) {
        return ModelResponse.builder()
            .hasToolCall(true).toolCallId(toolCallId).toolName(toolName).toolArguments(args).build();
    }

    public static ModelResponse timeout() {
        return ModelResponse.builder()
            .timedOut(true)
            .content("Oru nimisham — store pathi enna help venum?")
            .build();
    }
}
