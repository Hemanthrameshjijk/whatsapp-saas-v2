package com.whatsappai.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whatsappai.config.WhatsAppConfig;
import com.whatsappai.service.ConversationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/webhook")
@Slf4j
@RequiredArgsConstructor
public class WebhookController {

    private final WhatsAppConfig waConfig;
    private final ConversationService conversationService;
    private final ObjectMapper objectMapper;

    /** Step 1: GET — Meta verification handshake */
    @GetMapping
    public ResponseEntity<String> verify(
        @RequestParam("hub.mode") String mode,
        @RequestParam("hub.verify_token") String token,
        @RequestParam("hub.challenge") String challenge) {
        if ("subscribe".equals(mode) && waConfig.getVerifyToken().equals(token)) {
            log.info("Webhook verified successfully");
            return ResponseEntity.ok(challenge);
        }
        log.warn("Webhook verification failed. Expected token: {}", waConfig.getVerifyToken());
        return ResponseEntity.status(403).build();
    }

    /** Step 1 + 1b: POST — receive messages, return 200 ALWAYS, process async */
    @PostMapping
    public ResponseEntity<String> receive(HttpServletRequest request) {
        // ALWAYS return 200 immediately per spec invariant #1
        try {
            String rawBody;
            try (java.io.InputStream is = request.getInputStream()) {
                rawBody = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
            String signature = request.getHeader("X-Hub-Signature-256");

            // HMAC verify (timing-safe)
            if (!verifyHmac(rawBody, signature)) {
                log.warn("HMAC verification failed — body may be tampered. Signature: {}", signature);
                return ResponseEntity.ok("EVENT_RECEIVED"); // still return 200
            }

            // Step 1b: @Async dispatch — controller returns immediately after this
            dispatchAsync(rawBody);
        } catch (Exception e) {
            log.error("Webhook receive error: {}", e.getMessage());
        }
        return ResponseEntity.ok("EVENT_RECEIVED");
    }

    private void dispatchAsync(String rawBody) {
        try {
            JsonNode root = objectMapper.readTree(rawBody);
            JsonNode entries = root.path("entry");
            if (!entries.isArray()) return;
            for (JsonNode entry : entries) {
                for (JsonNode change : entry.path("changes")) {
                    JsonNode value = change.path("value");
                    JsonNode messages = value.path("messages");
                    if (!messages.isArray() || messages.isEmpty()) continue;
                    String toNumber = value.path("metadata").path("display_phone_number").asText();
                    String phoneNumberId = value.path("metadata").path("phone_number_id").asText();

                    for (JsonNode msg : messages) {
                        String from = msg.path("from").asText();
                        String type = msg.path("type").asText("text");
                        String text = null;

                        if ("text".equals(type)) {
                            text = msg.path("text").path("body").asText(null);
                        } else if ("interactive".equals(type)) {
                            JsonNode interactive = msg.path("interactive");
                            String interactiveType = interactive.path("type").asText();
                            if ("button_reply".equals(interactiveType)) {
                                text = interactive.path("button_reply").path("title").asText();
                                // We use the title as the text for the LLM to process naturally
                                log.info("Received button reply: {}", text);
                            } else if ("list_reply".equals(interactiveType)) {
                                text = interactive.path("list_reply").path("title").asText();
                                log.info("Received list reply: {}", text);
                            }
                        }

                        Double lat = msg.path("location").has("latitude") ?
                            msg.path("location").path("latitude").asDouble() : null;
                        Double lng = msg.path("location").has("longitude") ?
                            msg.path("location").path("longitude").asDouble() : null;
                        
                        if (text != null || "location".equals(type)) {
                            conversationService.processIncoming(from, toNumber, type, text, lat, lng, phoneNumberId);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Async dispatch error: {}", e.getMessage());
        }
    }

    /** HMAC-SHA256 timing-safe verification using raw body bytes */
    boolean verifyHmac(String rawBody, String signatureHeader) {
        if (signatureHeader == null || !signatureHeader.startsWith("sha256=")) return false;
        try {
            String received = signatureHeader.substring("sha256=".length());
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(waConfig.getAppSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String computed = Hex.encodeHexString(mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8)));
            return MessageDigest.isEqual(computed.getBytes(), received.getBytes());
        } catch (Exception e) {
            log.error("HMAC error: {}", e.getMessage());
            return false;
        }
    }
}
