package com.whatsappai.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "marketing_campaigns")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketingCampaign {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    private String name;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false)
    private String audience;

    @Column(name = "promo_id")
    private UUID promoId;

    @Column(name = "sent_count")
    private int sentCount;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
