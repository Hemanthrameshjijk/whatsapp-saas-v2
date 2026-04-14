package com.whatsappai.controller;

import com.whatsappai.repository.ChatMessageRepository;
import com.whatsappai.repository.CustomerRepository;
import com.whatsappai.service.SessionStore;
import com.whatsappai.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Dev-only controller to reset state for E2E tests.
 * Only active when profile 'test' or 'dev' is used (for safety).
 */
@RestController
@RequestMapping("/api/test-support")
@Profile({"dev", "test", "default"})
@RequiredArgsConstructor
public class TestSupportController {

    private final SessionStore sessionStore;
    private final CartService cartService;
    private final CustomerRepository customerRepository;
    private final ChatMessageRepository chatMessageRepository;

    @PostMapping("/reset")
    public ResponseEntity<String> resetCustomer(@RequestParam UUID bizId, @RequestParam String phone) {
        sessionStore.delete(bizId, phone);
        cartService.clearCart(bizId, phone);
        // Optional: clear messages and customer record for a truly 100% fresh start
        // chatMessageRepository.deleteByBusinessIdAndCustomerPhone(bizId, phone);
        return ResponseEntity.ok("State reset for " + phone);
    }
}
