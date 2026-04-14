package com.whatsappai.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whatsappai.ai.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class OllamaProvider implements AIModelProvider {

    @Value("${ai.ollama.base-url}")
    private String baseUrl;

    @Value("${ai.embed-model}")
    private String embedModel;

    @Value("${ai.temperature:0.4}")
    private double temperature;

    @Value("${ai.max-tokens:512}")
    private int maxTokens;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public ModelResponse complete(ModelRequest request) {
        try {
            return doComplete(request);
        } catch (ResourceAccessException e) {
            log.warn("Ollama timeout on first attempt, retrying with llama3.2:1b...");
            try {
                request.setModel("llama3.2:1b");
                return doComplete(request);
            } catch (Exception ex) {
                log.error("Ollama double timeout on fallback — returning safe fallback");
                return ModelResponse.timeout();
            }
        } catch (Exception e) {
            log.error("Ollama error: {}", e.getMessage());
            return ModelResponse.timeout();
        }
    }

    private ModelResponse doComplete(ModelRequest request) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", request.getModel() != null ? request.getModel() : "llama3.2:1b");
        body.put("stream", false);

        List<Map<String, Object>> messages = new ArrayList<>();
        if (request.getSystemPrompt() != null) {
            messages.add(Map.of("role", "system", "content", request.getSystemPrompt()));
        }
        for (Message m : request.getConversationHistory()) {
            Map<String, Object> msg = new LinkedHashMap<>();
            msg.put("role", m.getRole());
            msg.put("content", m.getContent() != null ? m.getContent() : "");
            messages.add(msg);
        }
        body.put("messages", messages);
        body.put("options", Map.of("temperature", request.getTemperature(), "num_predict", request.getMaxTokens()));

        if (!request.getTools().isEmpty()) {
            List<Map<String, Object>> tools = new ArrayList<>();
            for (ToolDefinition td : request.getTools()) {
                tools.add(Map.of(
                    "type", "function",
                    "function", Map.of(
                        "name", td.getFunction().getName(),
                        "description", td.getFunction().getDescription(),
                        "parameters", td.getFunction().getParameters()
                    )
                ));
            }
            body.put("tools", tools);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> resp = restTemplate.exchange(
            baseUrl + "/api/chat", HttpMethod.POST,
            new HttpEntity<>(objectMapper.writeValueAsString(body), headers), String.class);

        JsonNode root = objectMapper.readTree(resp.getBody());
        JsonNode msgNode = root.path("message");
        JsonNode toolCalls = msgNode.path("tool_calls");

        if (toolCalls.isArray() && !toolCalls.isEmpty()) {
            JsonNode tc = toolCalls.get(0);
            String toolName = tc.path("function").path("name").asText();
            Map<String, Object> args = objectMapper.convertValue(
                tc.path("function").path("arguments"), Map.class);
            String callId = UUID.randomUUID().toString();
            return ModelResponse.toolCall(callId, toolName, args);
        }

        String content = msgNode.path("content").asText();
        return ModelResponse.text(content);
    }

    @Override
    public float[] embed(String text) {
        try {
            Map<String, String> body = Map.of("model", embedModel, "prompt", text);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            ResponseEntity<String> resp = restTemplate.exchange(
                baseUrl + "/api/embeddings", HttpMethod.POST,
                new HttpEntity<>(objectMapper.writeValueAsString(body), headers), String.class);

            JsonNode root = objectMapper.readTree(resp.getBody());
            JsonNode embNode = root.path("embedding");
            float[] vec = new float[embNode.size()];
            for (int i = 0; i < vec.length; i++) vec[i] = (float) embNode.get(i).asDouble();
            return vec;
        } catch (Exception e) {
            log.error("Ollama embed error: {}", e.getMessage());
            return new float[384]; // zero vector fallback
        }
    }
}
