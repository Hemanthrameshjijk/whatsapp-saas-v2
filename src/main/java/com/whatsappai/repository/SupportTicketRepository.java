package com.whatsappai.repository;

import com.whatsappai.entity.SupportTicket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, UUID> {

    Page<SupportTicket> findByBusinessIdOrderByCreatedAtDesc(UUID businessId, Pageable pageable);

    Page<SupportTicket> findByBusinessIdAndStatusOrderByCreatedAtDesc(UUID businessId, String status, Pageable pageable);

    long countByBusinessIdAndStatus(UUID businessId, String status);

    Optional<SupportTicket> findByIdAndBusinessId(UUID id, UUID businessId);

    java.util.List<SupportTicket> findByBusinessIdAndCustomerPhoneOrderByCreatedAtDesc(UUID businessId, String phone);
}
