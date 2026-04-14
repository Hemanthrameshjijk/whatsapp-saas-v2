package com.whatsappai.qdrant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Component
@Slf4j
@RequiredArgsConstructor
public class QdrantClient {

    @Value("${qdrant.host}")
    private String host;

    @Value("${qdrant.port}")
    private int port;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private String baseUrl() {
        return "http://" + host + ":" + port;
    }

    public void createCollection(String name, int dimension) {
        try {
            String url = baseUrl() + "/collections/" + name;
            Map<String, Object> body = Map.of(
                "vectors", Map.of(
                    "size", dimension,
                    "distance", "Cosine"
                )
            );
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            restTemplate.exchange(url, HttpMethod.PUT,
                new HttpEntity<>(objectMapper.writeValueAsString(body), headers), String.class);
            log.info("Qdrant collection '{}' created (dim={})", name, dimension);
        } catch (HttpClientErrorException.Conflict e) {
            log.debug("Qdrant collection '{}' already exists — skipping", name);
        } catch (Exception e) {
            log.error("Qdrant createCollection '{}' failed: {}", name, e.getMessage());
        }
    }

    public void upsert(String collection, String pointId, float[] vector, Map<String, Object> payload) {
        try {
            List<Float> vecList = new ArrayList<>();
            for (float v : vector) vecList.add(v);

            Map<String, Object> point = Map.of("id", pointId, "vector", vecList, "payload", payload);
            Map<String, Object> body = Map.of("points", List.of(point));

            String url = baseUrl() + "/collections/" + collection + "/points";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            restTemplate.exchange(url, HttpMethod.PUT,
                new HttpEntity<>(objectMapper.writeValueAsString(body), headers), String.class);
        } catch (Exception e) {
            log.error("Qdrant upsert to '{}' failed: {}", collection, e.getMessage());
        }
    }

    public List<String> search(String collection, float[] vector, int limit, String customerPhone) {
        try {
            List<Float> vecList = new ArrayList<>();
            for (float v : vector) vecList.add(v);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("vector", vecList);
            body.put("limit", limit);
            body.put("with_payload", true);

            // Filter by customer phone
            body.put("filter", Map.of(
                "must", List.of(Map.of(
                    "key", "customer_phone",
                    "match", Map.of("value", customerPhone)
                ))
            ));

            String url = baseUrl() + "/collections/" + collection + "/points/search";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.POST,
                new HttpEntity<>(objectMapper.writeValueAsString(body), headers), String.class);

            JsonNode root = objectMapper.readTree(resp.getBody());
            List<String> results = new ArrayList<>();
            for (JsonNode r : root.path("result")) {
                String summaryText = r.path("payload").path("summary_text").asText();
                if (!summaryText.isBlank()) results.add(summaryText);
            }
            return results;
        } catch (Exception e) {
            log.warn("Qdrant search failed for '{}': {}", collection, e.getMessage());
            return Collections.emptyList();
        }
    }

    public boolean collectionExists(String name) {
        try {
            restTemplate.getForEntity(baseUrl() + "/collections/" + name, String.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
