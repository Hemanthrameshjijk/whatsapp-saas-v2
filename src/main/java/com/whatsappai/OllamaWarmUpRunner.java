package com.whatsappai;

import com.whatsappai.ai.ModelGateway;
import com.whatsappai.ai.OllamaProvider;
import com.whatsappai.ai.dto.Message;
import com.whatsappai.ai.dto.ModelRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class OllamaWarmUpRunner implements ApplicationRunner {

    private final ModelGateway modelGateway;
    private final OllamaProvider ollamaProvider;

    @Override
    public void run(ApplicationArguments args) {
        log.info("Warming up Ollama models...");
        try {
            modelGateway.complete(ModelRequest.builder()
                .model("llama3.2:3b")
                .systemPrompt("You are a helpful assistant.")
                .conversationHistory(List.of(Message.builder().role("user").content("hello").build()))
                .maxTokens(5).temperature(0.1).build());
            ollamaProvider.embed("warmup");
            log.info("Ollama warm-up complete ✓");
        } catch (Exception e) {
            log.warn("Ollama warm-up failed — will retry on first message. {}", e.getMessage());
        }
    }
}
