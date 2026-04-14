package com.whatsappai.controller;

import com.whatsappai.controller.WebhookController;
import com.whatsappai.config.WhatsAppConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class WebhookHmacTest {

    @Mock private WhatsAppConfig waConfig;
    @Mock private com.whatsappai.service.ConversationService conversationService;
    @Mock private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    private WebhookController controller;

    @BeforeEach
    void setUp() {
        controller = new WebhookController(waConfig, conversationService, objectMapper);
        org.mockito.Mockito.when(waConfig.getAppSecret()).thenReturn("testsecret123456789012345678901234");
    }

    @Test
    void verifyHmac_validSignature_returnsTrue() {
        String body = "{\"test\":\"data\"}";
        // Pre-compute expected HMAC
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec("testsecret123456789012345678901234".getBytes(), "HmacSHA256"));
            String computed = org.apache.commons.codec.binary.Hex.encodeHexString(mac.doFinal(body.getBytes()));
            assertTrue(controller.verifyHmac(body, "sha256=" + computed));
        } catch (Exception e) { fail(e); }
    }

    @Test
    void verifyHmac_tamperedBody_returnsFalse() {
        assertFalse(controller.verifyHmac("tampered", "sha256=abc123bad"));
    }

    @Test
    void verifyHmac_missingHeader_returnsFalse() {
        assertFalse(controller.verifyHmac("body", null));
    }

    @Test
    void verifyHmac_wrongPrefix_returnsFalse() {
        assertFalse(controller.verifyHmac("body", "md5=abc123"));
    }
}
