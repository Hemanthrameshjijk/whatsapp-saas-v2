package com.whatsappai.repository;

import com.whatsappai.entity.MarketingCampaign;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface MarketingCampaignRepository extends JpaRepository<MarketingCampaign, UUID> {
    Page<MarketingCampaign> findByBusinessIdOrderByCreatedAtDesc(UUID businessId, Pageable pageable);
}
