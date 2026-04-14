package com.whatsappai.model;

import java.time.LocalDateTime;

public interface ChatSummaryProjection {
    String getPhone();
    String getCustomerName();
    String getMessage();
    String getDirection();
    LocalDateTime getTimestamp();
    Boolean getRequiresHuman();
    String getRequiresHumanReason();
}
