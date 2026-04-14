package com.whatsappai.ai;

import com.whatsappai.ai.dto.ModelRequest;
import com.whatsappai.ai.dto.ModelResponse;

public interface AIModelProvider {
    /** Chat completion — never includes vision. */
    ModelResponse complete(ModelRequest request);

    /** Embed text using nomic-embed-text (384-dim). */
    float[] embed(String text);
}
