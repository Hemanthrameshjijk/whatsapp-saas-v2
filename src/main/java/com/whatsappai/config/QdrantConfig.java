package com.whatsappai.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class QdrantConfig {

    @Value("${ai.timeout-ms:8000}")
    private int timeoutMs;

    /** RestTemplate for Qdrant and Ollama — 8s read timeout per spec. */
    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000); // 10s
        factory.setReadTimeout(300000); // 300s — Large prompts on CPU inference need significant time
        return new RestTemplate(factory);
    }
}
