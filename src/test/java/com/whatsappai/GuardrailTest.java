package com.whatsappai;

import com.whatsappai.entity.AISettings;
import com.whatsappai.model.IntentResult;
import com.whatsappai.model.IntentType;
import com.whatsappai.service.GuardrailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GuardrailTest {

    private GuardrailService guardrailService;
    private AISettings settings;

    @BeforeEach
    void setUp() {
        guardrailService = new GuardrailService();
        settings = AISettings.builder().build();
    }

    @Test
    void politicsKeyword_blocked() {
        IntentResult result = guardrailService.classify("What do you think about Modi's policy?", settings);
        assertTrue(result.isBlocked());
        assertEquals("POLITICS", result.getBlockReason());
    }

    @Test
    void medicalKeyword_blocked() {
        IntentResult result = guardrailService.classify("I have a fever, what medicine should I take?", settings);
        assertTrue(result.isBlocked());
        assertEquals("MEDICAL", result.getBlockReason());
    }

    @Test
    void allowlist_skipsBlock() {
        settings.setGuardrailAllowlist("doctor");
        IntentResult result = guardrailService.classify("I want Doctor Salt please", settings);
        // "doctor" is in allowlist — should NOT be blocked
        assertFalse(result.isBlocked());
    }

    @Test
    void competitor_blocked() {
        settings.setCompetitorKeywords("BigBazaar,Flipkart");
        IntentResult result = guardrailService.classify("Is this cheaper than BigBazaar?", settings);
        assertTrue(result.isBlocked());
        assertEquals("COMPETITOR", result.getBlockReason());
    }

    @Test
    void normalMessage_notBlocked() {
        IntentResult result = guardrailService.classify("Show me your products", settings);
        assertFalse(result.isBlocked());
        assertEquals(IntentType.BROWSE, result.getIntent());
    }

    @Test
    void greeting_detected() {
        IntentResult result = guardrailService.classify("Hi there!", settings);
        assertEquals(IntentType.GREETING, result.getIntent());
        assertFalse(result.isBlocked());
    }

    @Test
    void sanitiser_priceHallucination_blocked() {
        var products = java.util.List.of(
            com.whatsappai.entity.Product.builder()
                .id(java.util.UUID.randomUUID())
                .businessId(java.util.UUID.randomUUID())
                .name("T-Shirt").price(new java.math.BigDecimal("599")).build()
        );
        String response = "This T-Shirt costs ₹9999 and is great quality!";
        String sanitised = guardrailService.sanitise(response, products, settings);
        // Price ₹9999 doesn't match ₹599 — fallback returned
        assertEquals("I can only help with our store — what would you like to order? 😊", sanitised);
    }

    @Test
    void sanitiser_correctPrice_passes() {
        var products = java.util.List.of(
            com.whatsappai.entity.Product.builder()
                .id(java.util.UUID.randomUUID())
                .businessId(java.util.UUID.randomUUID())
                .name("T-Shirt").price(new java.math.BigDecimal("599")).build()
        );
        String response = "This T-Shirt costs ₹599. Great choice!";
        String sanitised = guardrailService.sanitise(response, products, settings);
        assertEquals(response, sanitised);
    }
}
