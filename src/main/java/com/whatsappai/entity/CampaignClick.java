package com.whatsappai.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "marketing_clicks")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignClick {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "link_id", nullable = false)
    private UUID linkId;

    @Column(name = "customer_phone")
    private String customerPhone;

    @Column(name = "clicked_at", insertable = false, updatable = false)
    private LocalDateTime clickedAt;
}
