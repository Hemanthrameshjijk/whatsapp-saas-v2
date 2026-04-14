package com.whatsappai.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message {
    private String role;    // system / user / assistant / tool
    private String content;
    private String toolCallId; // for tool result messages
    private String name;       // tool name when role=tool
}
