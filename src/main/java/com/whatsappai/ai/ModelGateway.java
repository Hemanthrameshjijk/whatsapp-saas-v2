package com.whatsappai.ai;

import com.whatsappai.ai.dto.ModelRequest;
import com.whatsappai.ai.dto.ModelResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ModelGateway {

    private final OllamaProvider ollamaProvider;
    private final OpenAIProvider openAIProvider;

    public ModelResponse complete(ModelRequest request) {
        String model = request.getModel();
        if (model == null || model.isBlank() || model.startsWith("llama") || model.startsWith("mistral") || model.startsWith("gemma")) {
            return ollamaProvider.complete(request);
        }
        if (model.startsWith("gpt-") || model.startsWith("o1")) {
            return openAIProvider.complete(request);
        }
        log.warn("Unknown model '{}', falling back to Ollama", model);
        return ollamaProvider.complete(request);
    }

    public float[] embed(String text) {
        return ollamaProvider.embed(text);
    }
}
