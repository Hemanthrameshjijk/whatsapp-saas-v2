package com.whatsappai.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    @Column(name = "customer_id")
    private UUID customerId;

    @Column(name = "customer_phone", nullable = false)
    private String customerPhone;

    @Builder.Default
    private String status = "PENDING";

    @Column(name = "total_amount", precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "delivery_type")
    private String deliveryType;

    @Column(name = "delivery_address_text", columnDefinition = "TEXT")
    private String deliveryAddressText;

    @Column(name = "delivery_lat")
    private Double deliveryLat;

    @Column(name = "delivery_lng")
    private Double deliveryLng;

    @Column(name = "address_source")
    private String addressSource;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Builder.Default
    @Column(name = "payment_status")
    private String paymentStatus = "PENDING";

    @Column(name = "promo_code")
    private String promoCode;

    @Column(name = "discount_amount", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;
}
