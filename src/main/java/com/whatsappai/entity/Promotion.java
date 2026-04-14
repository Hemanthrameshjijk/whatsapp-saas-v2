package com.whatsappai.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "promotions",
       uniqueConstraints = @UniqueConstraint(columnNames = {"business_id", "code"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    @Column(nullable = false, length = 50)
    private String code;

    private String description;

    @Column(name = "discount_type", nullable = false)
    private String discountType; // PERCENTAGE or FLAT

    @Column(name = "discount_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "max_discount", precision = 10, scale = 2)
    private BigDecimal maxDiscount;

    @Column(name = "min_order_amount", precision = 10, scale = 2)
    private BigDecimal minOrderAmount;

    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "customer_phone")
    private String customerPhone;

    @Column(name = "first_order_only")
    @Builder.Default
    private Boolean firstOrderOnly = false;

    @Column(name = "max_uses")
    private Integer maxUses;

    @Column(name = "used_count")
    @Builder.Default
    private Integer usedCount = 0;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "starts_at")
    private LocalDateTime startsAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    // Offer-specific fields (offers = auto-apply without code)
    @Column(name = "auto_apply")
    @Builder.Default
    private Boolean autoApply = false;

    @Column(name = "offer_label", length = 100)
    private String offerLabel; // e.g. "🔥 10% OFF", "Buy 2 Get 1 Free"

    private String category; // target product category (e.g. "Electronics")

    @Column(name = "brand")
    private String brand; // target product brand (e.g. "Nike")
}
