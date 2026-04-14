package com.whatsappai.service;

import com.whatsappai.entity.ChatMessage;
import com.whatsappai.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Dedicated service for persisting chat messages.
 *
 * WHY THIS EXISTS:
 * ConversationService.processIncoming() is annotated with @Async, which means it runs
 * on a separate thread-pool thread. Calling a @Transactional method on the SAME bean
 * (self-invocation via "this.method()") bypasses Spring's AOP proxy — the transaction
 * is never opened and repository saves silently fail or operate outside a transaction.
 *
 * By extracting persistMessage() into THIS separate Spring bean, the call goes through
 * the proxy and @Transactional is honoured correctly.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ChatPersistenceService {

    private final ChatMessageRepository chatMessageRepository;

    /**
     * Saves an inbound + outbound message pair atomically.
     * Each call to this method runs in its own transaction.
     */
    @Transactional
    public void persistMessage(UUID businessId, String phone,
                               String inbound, String outbound,
                               String sessionId,
                               boolean guardrailTriggered, String guardrailReason) {
        try {
            if (inbound != null) {
                chatMessageRepository.save(ChatMessage.builder()
                    .businessId(businessId)
                    .customerPhone(phone)
                    .direction("IN")
                    .content(inbound)
                    .sessionId(sessionId)
                    .guardrailTriggered(guardrailTriggered)
                    .guardrailReason(guardrailReason)
                    .build());
            }
            if (outbound != null) {
                chatMessageRepository.save(ChatMessage.builder()
                    .businessId(businessId)
                    .customerPhone(phone)
                    .direction("OUT")
                    .content(outbound)
                    .sessionId(sessionId)
                    .build());
            }
            log.debug("Persisted message pair — phone={} biz={} guardrail={}",
                phone, businessId, guardrailTriggered);
        } catch (Exception e) {
            log.error("CRITICAL: Failed to persist message for phone={} biz={}: {}",
                phone, businessId, e.getMessage(), e);
        }
    }
}
