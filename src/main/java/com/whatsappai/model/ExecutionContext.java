package com.whatsappai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionContext {
    private UUID biz;
    private String phone;
    private String sessionId;
    private UUID customerId;
    private String deliveryAddress;
    private Double deliveryLat;
    private Double deliveryLng;
}
