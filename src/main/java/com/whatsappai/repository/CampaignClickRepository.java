package com.whatsappai.repository;

import com.whatsappai.entity.CampaignClick;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.List;

public interface CampaignClickRepository extends JpaRepository<CampaignClick, UUID> {
    List<CampaignClick> findByLinkId(UUID linkId);
}
