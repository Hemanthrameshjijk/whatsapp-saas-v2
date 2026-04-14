package com.whatsappai.service;

import com.whatsappai.entity.AISettings;
import com.whatsappai.entity.Product;
import com.whatsappai.entity.SupportTicket;
import com.whatsappai.repository.AISettingsRepository;
import com.whatsappai.repository.ProductRepository;
import com.whatsappai.repository.SupportTicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class SupportTicketService {

    private final SupportTicketRepository ticketRepository;
    private final ProductRepository productRepository;
    private final AISettingsRepository aiSettingsRepository;

    /**
     * Validates the product's policy and creates a ticket if eligible.
     * Returns the ticket if created, or null with an error message if not eligible.
     */
    @Transactional
    public TicketResult createTicket(UUID bizId, UUID customerId, String phone,
                                      UUID productId, UUID orderId,
                                      String type, String reason) {

        // Fetch product to check policy
        Optional<Product> productOpt = productRepository.findByIdAndBusinessId(productId, bizId);
        if (productOpt.isEmpty()) {
            return TicketResult.error("Product not found. Please provide a valid product.");
        }
        Product product = productOpt.get();
        AISettings settings = aiSettingsRepository.findByBusinessId(bizId).orElseThrow();

        String policyApplied = null;

        if ("RETURN".equals(type)) {
            // Check return eligibility
            String returnMode = product.getReturnMode() != null ? product.getReturnMode() : "GLOBAL";
            if ("NONE".equals(returnMode)) {
                return TicketResult.error("Sorry, " + product.getName() + " is a non-returnable product. Returns are not accepted for this item.");
            } else if ("CUSTOM".equals(returnMode)) {
                policyApplied = product.getCustomReturnPolicy();
            } else {
                // GLOBAL
                if ("DISABLED".equals(settings.getGlobalReturnMode())) {
                    return TicketResult.error("Sorry, our store does not currently accept returns.");
                }
                policyApplied = settings.getGlobalReturnPolicy();
            }
        } else if ("WARRANTY".equals(type)) {
            // Check warranty eligibility
            String warrantyMode = product.getWarrantyMode() != null ? product.getWarrantyMode() : "GLOBAL";
            if ("NONE".equals(warrantyMode)) {
                return TicketResult.error("Sorry, " + product.getName() + " does not come with a warranty.");
            } else if ("CUSTOM".equals(warrantyMode)) {
                policyApplied = product.getWarrantyDetails();
            } else {
                // GLOBAL
                if ("DISABLED".equals(settings.getGlobalWarrantyMode())) {
                    return TicketResult.error("Sorry, our store does not currently offer warranty coverage.");
                }
                policyApplied = settings.getGlobalWarrantyDetails();
            }
        }

        SupportTicket ticket = SupportTicket.builder()
            .businessId(bizId)
            .customerId(customerId)
            .customerPhone(phone)
            .orderId(orderId)
            .productId(productId)
            .type(type)
            .reason(reason)
            .productName(product.getName())
            .policyApplied(policyApplied != null ? policyApplied : "Store default policy")
            .build();

        SupportTicket saved = ticketRepository.save(ticket);
        log.info("Support ticket created: {} type={} product={}", saved.getId(), type, product.getName());
        return TicketResult.success(saved);
    }

    @Transactional
    public void updateStatus(UUID ticketId, UUID bizId, String status, String adminNotes) {
        ticketRepository.findByIdAndBusinessId(ticketId, bizId).ifPresent(t -> {
            t.setStatus(status);
            if (adminNotes != null) t.setAdminNotes(adminNotes);
            if ("RESOLVED".equals(status) || "REJECTED".equals(status) || "APPROVED".equals(status)) {
                t.setResolvedAt(LocalDateTime.now());
            }
            ticketRepository.save(t);
        });
    }

    public Page<SupportTicket> findAll(UUID bizId, Pageable pageable) {
        return ticketRepository.findByBusinessIdOrderByCreatedAtDesc(bizId, pageable);
    }

    public Page<SupportTicket> findByStatus(UUID bizId, String status, Pageable pageable) {
        return ticketRepository.findByBusinessIdAndStatusOrderByCreatedAtDesc(bizId, status, pageable);
    }

    public long countOpen(UUID bizId) {
        return ticketRepository.countByBusinessIdAndStatus(bizId, "OPEN");
    }

    public java.util.List<SupportTicket> findByPhone(UUID bizId, String phone) {
        return ticketRepository.findByBusinessIdAndCustomerPhoneOrderByCreatedAtDesc(bizId, phone);
    }

    // --- Inner result class ---
    public record TicketResult(boolean ok, String message, SupportTicket ticket) {
        public static TicketResult success(SupportTicket ticket) {
            return new TicketResult(true, null, ticket);
        }
        public static TicketResult error(String message) {
            return new TicketResult(false, message, null);
        }
    }
}
