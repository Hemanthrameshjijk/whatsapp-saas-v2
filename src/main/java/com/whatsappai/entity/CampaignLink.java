package com.whatsappai.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "marketing_links")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignLink {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "campaign_id")
    private UUID campaignId;

    @Column(name = "business_id")
    private UUID businessId;

    @Column(name = "influencer_name")
    private String influencerName;

    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "target_url", columnDefinition = "TEXT")
    private String targetUrl;

    @Column(name = "original_url", columnDefinition = "TEXT")
    private String originalUrl;

    @Column(name = "short_code", nullable = false, unique = true)
    private String shortCode;

    @Column(name = "click_count")
    private int clickCount;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
