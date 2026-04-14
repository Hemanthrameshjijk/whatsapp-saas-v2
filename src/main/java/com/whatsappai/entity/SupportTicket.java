package com.whatsappai.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "support_tickets")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupportTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    @Column(name = "customer_id")
    private UUID customerId;

    @Column(name = "customer_phone", nullable = false)
    private String customerPhone;

    @Column(name = "order_id")
    private UUID orderId;

    @Column(name = "product_id")
    private UUID productId;

    @Column(nullable = false)
    private String type; // RETURN or WARRANTY

    @Builder.Default
    private String status = "OPEN";

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "product_name")
    private String productName;

    @Column(name = "policy_applied", columnDefinition = "TEXT")
    private String policyApplied;

    @Column(name = "admin_notes", columnDefinition = "TEXT")
    private String adminNotes;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
}
