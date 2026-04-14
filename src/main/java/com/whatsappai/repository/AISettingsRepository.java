package com.whatsappai.repository;

import com.whatsappai.entity.AISettings;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface AISettingsRepository extends JpaRepository<AISettings, UUID> {
    Optional<AISettings> findByBusinessId(UUID businessId);
}
