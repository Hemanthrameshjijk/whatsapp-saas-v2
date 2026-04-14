package com.whatsappai.repository;

import com.whatsappai.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;
import com.whatsappai.model.ChatSummaryProjection;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    /** Used by DashboardController for paginated phone-filtered conversation view */
    Page<ChatMessage> findByBusinessIdAndCustomerPhoneOrderByCreatedAtDesc(UUID businessId, String phone, Pageable pageable);

    /** Plain list for internal recent-message lookups */
    @Query("SELECT c FROM ChatMessage c WHERE c.businessId = :biz AND c.customerPhone = :phone ORDER BY c.createdAt DESC")
    List<ChatMessage> findRecentByBusinessIdAndPhone(@Param("biz") UUID businessId, @Param("phone") String phone, Pageable pageable);

    Page<ChatMessage> findByBusinessId(UUID businessId, Pageable pageable);

    @Query(value = "SELECT * FROM (" +
                   "  SELECT DISTINCT ON (c.customer_phone) " +
                   "         c.customer_phone as phone, " +
                   "         u.name as customerName, " +
                   "         c.content as message, " +
                   "         c.direction as direction, " +
                   "         c.created_at as timestamp, " +
                   "         u.requires_human as requiresHuman, " +
                   "         u.requires_human_reason as requiresHumanReason " +
                   "  FROM chat_messages c " +
                   "  LEFT JOIN customers u ON c.customer_phone = u.phone AND c.business_id = u.business_id " +
                   "  WHERE c.business_id = :bizId " +
                   "  ORDER BY c.customer_phone, c.created_at DESC " +
                   ") t ORDER BY t.timestamp DESC", nativeQuery = true)
    List<ChatSummaryProjection> findRecentChatSummaries(@Param("bizId") UUID businessId);


    @Query("SELECT c FROM ChatMessage c WHERE c.businessId = :biz AND c.guardrailTriggered = true ORDER BY c.createdAt DESC")
    Page<ChatMessage> findGuardrailTriggers(@Param("biz") UUID businessId, Pageable pageable);

    @Query("SELECT c FROM ChatMessage c WHERE c.businessId = :biz AND c.guardrailTriggered = true AND c.guardrailReason = :reason ORDER BY c.createdAt DESC")
    Page<ChatMessage> findGuardrailTriggersByReason(@Param("biz") UUID businessId, @Param("reason") String reason, Pageable pageable);

    @Query("SELECT COUNT(c) FROM ChatMessage c WHERE c.businessId = :biz AND c.guardrailTriggered = true")
    long countGuardrailTriggers(@Param("biz") UUID businessId);

}
