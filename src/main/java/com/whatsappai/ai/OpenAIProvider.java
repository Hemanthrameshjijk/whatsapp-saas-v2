package com.whatsappai.ai;

import com.whatsappai.ai.dto.ModelRequest;
import com.whatsappai.ai.dto.ModelResponse;
import org.springframework.stereotype.Service;

/** Stub — OpenAI integration not implemented. Throws on any call. */
@Service
public class OpenAIProvider implements AIModelProvider {

    @Override
    public ModelResponse complete(ModelRequest request) {
        throw new UnsupportedOperationException("OpenAI provider not implemented. Use OllamaProvider.");
    }

    @Override
    public float[] embed(String text) {
        throw new UnsupportedOperationException("OpenAI embed not implemented. Use OllamaProvider.");
    }
}
