package com.whatsappai.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "ai_settings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AISettings {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "business_id", unique = true, nullable = false)
    private UUID businessId;

    @Builder.Default
    @Column(name = "tone_style")
    private String toneStyle = "friendly";

    @Builder.Default
    @Column(name = "language_style")
    private String languageStyle = "auto";

    @Builder.Default
    @Column(name = "upsell_mode")
    private String upsellMode = "soft";

    @Column(name = "greeting_template", columnDefinition = "TEXT")
    private String greetingTemplate;

    @Builder.Default
    @Column(name = "active_model")
    private String activeModel = "llama3.2:1b";

    @Column(name = "open_time")
    private LocalTime openTime;

    @Column(name = "close_time")
    private LocalTime closeTime;

    @Builder.Default
    @Column(name = "delivery_enabled")
    private Boolean deliveryEnabled = true;

    @Builder.Default
    @Column(name = "delivery_charge", precision = 10, scale = 2)
    private BigDecimal deliveryCharge = BigDecimal.ZERO;

    @Column(name = "free_delivery_above", precision = 10, scale = 2)
    private BigDecimal freeDeliveryAbove;

    @Builder.Default
    @Column(name = "estimated_delivery_days")
    private Integer estimatedDeliveryDays = 2;

    @Column(name = "upi_id")
    private String upiId;

    @Column(name = "shop_address", columnDefinition = "TEXT")
    private String shopAddress;

    @Column(name = "guardrail_allowlist", columnDefinition = "TEXT")
    private String guardrailAllowlist;

    @Column(name = "competitor_keywords", columnDefinition = "TEXT")
    private String competitorKeywords;

    @Column(name = "global_return_policy", columnDefinition = "TEXT")
    private String globalReturnPolicy;

    @Column(name = "global_return_mode")
    @Builder.Default
    private String globalReturnMode = "ENABLED";

    @Column(name = "global_warranty_details", columnDefinition = "TEXT")
    private String globalWarrantyDetails;

    @Column(name = "global_warranty_claim_rules", columnDefinition = "TEXT")
    private String globalWarrantyClaimRules;

    @Column(name = "global_warranty_mode")
    @Builder.Default
    private String globalWarrantyMode = "ENABLED";

    @Column(name = "general_faq", columnDefinition = "TEXT")
    private String generalFaq;

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
