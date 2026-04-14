package com.whatsappai.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "businesses")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Business {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "whatsapp_number", unique = true, nullable = false)
    private String whatsappNumber;

    private String city;

    @Column(name = "business_type")
    private String businessType;

    @Column(name = "phone_number_id")
    private String phoneNumberId;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
