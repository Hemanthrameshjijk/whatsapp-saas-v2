package com.whatsappai.service;

import com.whatsappai.entity.ChatMessage;
import com.whatsappai.model.ConversationSession;
import com.whatsappai.model.ConvStage;
import com.whatsappai.model.SessionMessage;
import com.whatsappai.repository.ChatMessageRepository;
import com.whatsappai.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class SessionStore {

    @Value("${redis.session-ttl-minutes:30}")
    private long sessionTtlMinutes;

    private final RedisTemplate<String, Object> redisTemplate;
    private final ChatMessageRepository chatMessageRepo;
    private final CustomerRepository customerRepo;

    private String key(UUID biz, String phone) {
        return "session:" + biz + ":" + phone;
    }

    public ConversationSession getOrRebuild(UUID biz, String phone) {
        String key = key(biz, phone);
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached instanceof ConversationSession session) return session;
        } catch (Exception e) {
            log.warn("Redis session lookup failed for {} (is Redis down?): {}", key, e.getMessage());
        }

        // Redis miss or error — rebuild from PostgreSQL
        log.debug("Session miss for {}:{} — rebuilding from PG", biz, phone);
        ConversationSession session = new ConversationSession();
        List<ChatMessage> msgs = chatMessageRepo
            .findRecentByBusinessIdAndPhone(biz, phone, PageRequest.of(0, 20));
        // Reverse to chronological order
        List<SessionMessage> history = msgs.stream()
            .map(SessionMessage::from)
            .sorted((a, b) -> a.getTimestamp().compareTo(b.getTimestamp()))
            .toList();
        session.setMessages(new java.util.ArrayList<>(history));

        customerRepo.findByBusinessIdAndPhone(biz, phone).ifPresent(c -> {
            session.setCustomerName(c.getName());
            if (c.getName() != null || c.getTotalOrders() > 0) {
                session.setStage(ConvStage.BROWSING);
            } else if (!msgs.isEmpty()) {
                // Customer has no name but has chat history — already greeted
                session.setStage(ConvStage.AWAITING_NAME);
            } else {
                session.setStage(ConvStage.GREETING);
            }
        });

        save(biz, phone, session);
        return session;
    }

    public void save(UUID biz, String phone, ConversationSession session) {
        try {
            redisTemplate.opsForValue().set(key(biz, phone), session, sessionTtlMinutes, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("Could not save session to Redis for {}: {}", phone, e.getMessage());
        }
    }

    public void delete(UUID biz, String phone) {
        try {
            redisTemplate.delete(key(biz, phone));
        } catch (Exception e) {
            log.error("Could not delete session from Redis for {}: {}", phone, e.getMessage());
        }
    }
}
