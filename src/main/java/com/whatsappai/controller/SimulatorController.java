package com.whatsappai.controller;

import com.whatsappai.entity.Business;
import com.whatsappai.repository.BusinessRepository;
import com.whatsappai.security.JwtService;
import com.whatsappai.service.ConversationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/simulator")
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
@RequiredArgsConstructor
public class SimulatorController {

    private final JwtService jwtService;
    private final BusinessRepository businessRepository;
    private final ConversationService conversationService;

    @PostMapping("/send")
    public ResponseEntity<Map<String, String>> simulateMessage(
            Authentication auth,
            @RequestBody Map<String, String> payload) {
        
        UUID businessId = jwtService.extractBusinessId(auth);
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new RuntimeException("Business not found"));

        String fromPhone = payload.get("phone");
        String text = payload.get("text");
        String type = payload.getOrDefault("type", "text");
        Double lat = payload.get("lat") != null ? Double.parseDouble(payload.get("lat")) : null;
        Double lng = payload.get("lng") != null ? Double.parseDouble(payload.get("lng")) : null;

        log.info("Simulating message for business {}: from={}, text={}", businessId, fromPhone, text);

        // Inject into the standard pipeline
        conversationService.processIncoming(
                fromPhone,
                business.getWhatsappNumber(), // toNumber used for tenant resolution
                type,
                text,
                lat,
                lng,
                "SIM-" + UUID.randomUUID().toString()
        );

        return ResponseEntity.ok(Map.of("status", "SENT", "message", "Simulation triggered"));
    }
}
