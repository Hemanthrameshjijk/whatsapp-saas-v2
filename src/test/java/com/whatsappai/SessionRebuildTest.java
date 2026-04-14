package com.whatsappai;

import com.whatsappai.entity.ChatMessage;
import com.whatsappai.model.ConversationSession;
import com.whatsappai.model.ConvStage;
import com.whatsappai.model.SessionMessage;
import com.whatsappai.repository.ChatMessageRepository;
import com.whatsappai.repository.CustomerRepository;
import com.whatsappai.service.SessionStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionRebuildTest {

    @Mock RedisTemplate<String, Object> redisTemplate;
    @Mock ChatMessageRepository chatMessageRepo;
    @Mock CustomerRepository customerRepo;
    @Mock ValueOperations<String, Object> valueOps;
    @InjectMocks SessionStore sessionStore;

    @Test
    void redisMiss_rebuildsFromPostgres_seamlessly() {
        UUID biz = UUID.randomUUID();
        String phone = "+919876543210";

        // Redis miss
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("session:" + biz + ":" + phone)).thenReturn(null);

        // PostgreSQL has 3 messages
        List<ChatMessage> msgs = List.of(
            ChatMessage.builder().direction("IN").content("hi").createdAt(LocalDateTime.now().minusMinutes(5)).build(),
            ChatMessage.builder().direction("OUT").content("Hello!").createdAt(LocalDateTime.now().minusMinutes(4)).build(),
            ChatMessage.builder().direction("IN").content("show products").createdAt(LocalDateTime.now().minusMinutes(3)).build()
        );
        when(chatMessageRepo.findRecentByBusinessIdAndPhone(
            eq(biz), eq(phone), any(Pageable.class))).thenReturn(msgs);
        when(customerRepo.findByBusinessIdAndPhone(biz, phone)).thenReturn(Optional.empty());

        ConversationSession session = sessionStore.getOrRebuild(biz, phone);

        assertNotNull(session);
        assertEquals(3, session.getMessages().size());
        assertEquals(ConvStage.GREETING, session.getStage());
        verify(valueOps).set(eq("session:" + biz + ":" + phone), any(), anyLong(), any());
    }
}
