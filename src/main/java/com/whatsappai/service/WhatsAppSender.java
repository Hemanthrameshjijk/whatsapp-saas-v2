package com.whatsappai.service;

import com.whatsappai.config.WhatsAppConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class WhatsAppSender {

    private final WhatsAppConfig waConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public void sendText(String to, String body, String phoneNumberId) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("messaging_product", "whatsapp");
            payload.put("to", to);
            payload.put("type", "text");
            payload.put("text", Map.of("body", body));
            post(payload, phoneNumberId);
        } catch (Exception e) {
            log.error("WA sendText failed to {}: {}", to, e.getMessage());
        }
    }

    public void sendButtons(String to, String bodyText, List<Map<String, String>> buttons, String phoneNumberId) {
        try {
            List<Map<String, Object>> btnList = new ArrayList<>();
            for (Map<String, String> btn : buttons) {
                btnList.add(Map.of("type", "reply",
                    "reply", Map.of("id", btn.get("id"), "title", btn.get("title"))));
            }
            Map<String, Object> payload = Map.of(
                "messaging_product", "whatsapp",
                "to", to,
                "type", "interactive",
                "interactive", Map.of(
                    "type", "button",
                    "body", Map.of("text", bodyText),
                    "action", Map.of("buttons", btnList)
                )
            );
            post(payload, phoneNumberId);
        } catch (Exception e) {
            log.error("WA sendButtons failed to {}: {}", to, e.getMessage());
        }
    }

    public void sendList(String to, String header, String body, List<Map<String, Object>> sections, String phoneNumberId) {
        try {
            Map<String, Object> payload = Map.of(
                "messaging_product", "whatsapp",
                "to", to,
                "type", "interactive",
                "interactive", Map.of(
                    "type", "list",
                    "header", Map.of("type", "text", "text", header),
                    "body", Map.of("text", body),
                    "action", Map.of("button", "View Options", "sections", sections)
                )
            );
            post(payload, phoneNumberId);
        } catch (Exception e) {
            log.error("WA sendList failed to {}: {}", to, e.getMessage());
        }
    }

    public void sendImageCaption(String to, String imageUrl, String caption, String phoneNumberId) {
        try {
            Map<String, Object> payload = Map.of(
                "messaging_product", "whatsapp",
                "to", to,
                "type", "image",
                "image", Map.of("link", imageUrl, "caption", caption)
            );
            post(payload, phoneNumberId);
        } catch (Exception e) {
            log.error("WA sendImageCaption failed to {}: {}", to, e.getMessage());
        }
    }

    private void post(Map<String, Object> payload, String phoneNumberId) throws Exception {
        String token = waConfig.getAccessToken();
        if (token == null || token.isBlank() || "test".equalsIgnoreCase(token)) {
            log.info("WA TEST MODE: Skipping API call. Payload: {}", objectMapper.writeValueAsString(payload));
            return;
        }

        String url = waConfig.getApiUrl() + "/" + phoneNumberId + "/messages";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        String json = objectMapper.writeValueAsString(payload);
        ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.POST,
            new HttpEntity<>(json, headers), String.class);
        if (!resp.getStatusCode().is2xxSuccessful()) {
            log.warn("WA API non-2xx: {} — {}", resp.getStatusCode(), resp.getBody());
        }
    }
}
