package com.whatsappai.repository;

import com.whatsappai.entity.Promotion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface PromotionRepository extends JpaRepository<Promotion, UUID> {

    Optional<Promotion> findByBusinessIdAndCodeAndIsActiveTrue(UUID businessId, String code);

    Page<Promotion> findByBusinessIdOrderByCreatedAtDesc(UUID businessId, Pageable pageable);

    Optional<Promotion> findByIdAndBusinessId(UUID id, UUID businessId);

    @Modifying
    @Query("UPDATE Promotion p SET p.usedCount = p.usedCount + 1 WHERE p.id = :id")
    void incrementUsedCount(UUID id);

    @Query("SELECT p FROM Promotion p WHERE p.businessId = :bizId AND p.isActive = true AND p.autoApply = true " +
           "AND (p.startsAt IS NULL OR p.startsAt <= CURRENT_TIMESTAMP) " +
           "AND (p.expiresAt IS NULL OR p.expiresAt >= CURRENT_TIMESTAMP) " +
           "AND (p.maxUses IS NULL OR p.usedCount < p.maxUses)")
    java.util.List<Promotion> findActiveOffers(UUID bizId);
}
