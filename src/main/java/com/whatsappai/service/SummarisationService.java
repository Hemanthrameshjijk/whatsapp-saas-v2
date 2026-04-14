package com.whatsappai.service;

import com.whatsappai.ai.ModelGateway;
import com.whatsappai.ai.OllamaProvider;
import com.whatsappai.ai.dto.Message;
import com.whatsappai.ai.dto.ModelRequest;
import com.whatsappai.ai.dto.ModelResponse;
import com.whatsappai.model.ConversationSession;
import com.whatsappai.qdrant.QdrantClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class SummarisationService {

    private final ModelGateway modelGateway;
    private final OllamaProvider ollamaProvider;
    private final QdrantClient qdrantClient;

    /** @Async — NEVER blocks the reply path. */
    @Async("taskExecutor")
    public void summariseAndStore(UUID businessId, String customerPhone, ConversationSession session) {
        try {
            if (session.getMessages().size() < 3) return;

            // Build history text for summarisation
            StringBuilder history = new StringBuilder();
            for (var msg : session.getLast(10)) {
                history.append(msg.getRole()).append(": ").append(msg.getContent()).append("\n");
            }

            // LLM summarise
            ModelRequest req = ModelRequest.builder()
                .model("llama3.2:1b")
                .systemPrompt("Summarise this customer shopping conversation in 2-3 sentences. Focus on: preferences shown, products discussed, orders placed. Be concise.")
                .conversationHistory(List.of(Message.builder().role("user").content(history.toString()).build()))
                .maxTokens(150).temperature(0.3).build();

            ModelResponse resp = modelGateway.complete(req);
            if (resp.isTimedOut() || resp.getContent() == null) return;

            String summary = resp.getContent();

            // Embed with nomic-embed-text (384-dim)
            float[] vector = ollamaProvider.embed(summary);

            // Upsert to Qdrant
            String pointId = UUID.nameUUIDFromBytes(
                (businessId + ":" + customerPhone + ":" + System.currentTimeMillis()).getBytes()
            ).toString();

            qdrantClient.upsert(
                "memories_" + businessId, pointId, vector,
                Map.of(
                    "business_id", businessId.toString(),
                    "customer_phone", customerPhone,
                    "summary_text", summary,
                    "memory_type", "conversation",
                    "created_at", java.time.LocalDateTime.now().toString()
                )
            );
            log.debug("Memory stored for {}:{}", businessId, customerPhone);
        } catch (Exception e) {
            log.error("Summarisation async error: {}", e.getMessage());
        }
    }
}
