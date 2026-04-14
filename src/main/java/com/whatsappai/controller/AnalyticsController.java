package com.whatsappai.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.whatsappai.repository.ChatMessageRepository;
import com.whatsappai.service.TenantResolver;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final ChatMessageRepository chatMessageRepository;
    private final TenantResolver tenantResolver;

    @GetMapping("/active")
    public ResponseEntity<Map<String, Object>> getActiveStatus() {
        return ResponseEntity.ok(Map.of("status", "healthy", "baseline", "V23"));
    }
}
