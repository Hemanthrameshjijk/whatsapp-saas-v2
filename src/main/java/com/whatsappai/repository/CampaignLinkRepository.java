package com.whatsappai.repository;

import com.whatsappai.entity.CampaignLink;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;
import java.util.List;

public interface CampaignLinkRepository extends JpaRepository<CampaignLink, UUID> {
    Optional<CampaignLink> findByShortCode(String shortCode);
    List<CampaignLink> findByCampaignId(UUID campaignId);
    List<CampaignLink> findByBusinessId(UUID businessId);
}
