package com.whatsappai.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "customer_preferences",
       uniqueConstraints = @UniqueConstraint(columnNames = {"business_id", "customer_phone", "pref_key"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    @Column(name = "customer_phone", nullable = false)
    private String customerPhone;

    @Column(name = "pref_key", nullable = false)
    private String prefKey;

    @Column(name = "pref_value", columnDefinition = "TEXT")
    private String prefValue;

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
