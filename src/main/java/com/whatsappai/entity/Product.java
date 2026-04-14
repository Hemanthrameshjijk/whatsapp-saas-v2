package com.whatsappai.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "products")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    private String category;

    @Column(name = "sku")
    private String sku;

    @Column(name = "brand")
    private String brand;

    @Column(name = "type")
    private String type;

    @Column(name = "color")
    private String color;

    @Column(name = "size")
    private String size;
    @Column(name = "stock_qty")
    @Builder.Default
    private Integer stockQty = 0;

    @Column(name = "photo_url")
    private String photoUrl;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "out_of_stock")
    @Builder.Default
    private Boolean outOfStock = false;

    @Column(name = "warranty_mode")
    @Builder.Default
    private String warrantyMode = "GLOBAL";

    @Column(name = "warranty_details")
    private String warrantyDetails;

    @Column(name = "warranty_claim_rules", length = 500)
    private String warrantyClaimRules;

    @Column(name = "return_mode")
    @Builder.Default
    private String returnMode = "GLOBAL";

    @Column(name = "custom_return_policy", length = 500)
    private String customReturnPolicy;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
