package com.whatsappai.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "customers",
       uniqueConstraints = @UniqueConstraint(columnNames = {"business_id", "phone"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    @Column(nullable = false)
    private String phone;

    private String name;

    @Column(name = "is_blocked")
    @Builder.Default
    private boolean blocked = false;

    @Column(name = "total_orders")
    @Builder.Default
    private Integer totalOrders = 0;

    @Column(name = "requires_human")
    @Builder.Default
    private Boolean requiresHuman = false;

    @Column(name = "requires_human_reason")
    private String requiresHumanReason;

    @Column(name = "last_delivery_address", columnDefinition = "TEXT")
    private String lastDeliveryAddress;

    @Column(name = "last_lat")
    private Double lastLat;

    @Column(name = "last_lng")
    private Double lastLng;

    @Column(name = "referred_by")
    private String referredBy;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
