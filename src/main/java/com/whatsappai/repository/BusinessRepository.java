package com.whatsappai.repository;

import com.whatsappai.entity.Business;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface BusinessRepository extends JpaRepository<Business, UUID> {
    Optional<Business> findByWhatsappNumber(String whatsappNumber);
}
