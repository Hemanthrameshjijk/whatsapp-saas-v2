package com.whatsappai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntentResult {
    private IntentType intent;
    private boolean blocked;
    private String blockReason;
    private String safeReply;

    public static IntentResult of(IntentType intent) {
        return IntentResult.builder()
            .intent(intent)
            .blocked(false)
            .build();
    }

    public static IntentResult blocked(IntentType reason, String safeReply) {
        return IntentResult.builder()
            .intent(reason)
            .blocked(true)
            .blockReason(reason.name())
            .safeReply(safeReply)
            .build();
    }
}
