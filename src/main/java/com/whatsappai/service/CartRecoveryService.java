package com.whatsappai.service;

import com.whatsappai.entity.AISettings;
import com.whatsappai.entity.Business;
import com.whatsappai.model.CartItem;
import com.whatsappai.repository.AISettingsRepository;
import com.whatsappai.repository.BusinessRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.whatsappai.service.WhatsAppSender;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class CartRecoveryService {

    private final CartService cartService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final WhatsAppSender whatsAppSender;
    private final BusinessRepository businessRepository;
    private final AISettingsRepository aiSettingsRepository;
    private final PromotionService promotionService;
    private final com.whatsappai.repository.CustomerRepository customerRepository;

    private static final String NUDGE_TRACKER_KEY = "cart:nudge:"; // cart:nudge:{bizId}:{phone}

    /**
     * Run every 5 minutes to check for abandoned carts.
     * Logic: If a cart is non-empty and hasn't been touched for > 30 mins, send a nudge.
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void scanAndRecover() {
        try {
            log.debug("Starting Abandoned Cart Recovery scan...");
            
            Iterable<Business> businesses = businessRepository.findAll();
            for (Business biz : businesses) {
                processBusinessCarts(biz);
            }
        } catch (Exception e) {
            log.error("Aborted Cart Recovery scan due to service error (likely Redis connection): {}", e.getMessage());
        }
    }

    private void processBusinessCarts(Business biz) {
        Map<String, Map<String, CartItem>> activeCarts = cartService.getActiveCarts(biz.getId());
        if (activeCarts.isEmpty()) return;

        AISettings settings = aiSettingsRepository.findByBusinessId(biz.getId()).orElse(null);
        if (settings == null) return;

        for (Map.Entry<String, Map<String, CartItem>> entry : activeCarts.entrySet()) {
            String phone = entry.getKey();
            Map<String, CartItem> cart = entry.getValue();

            if (cart.isEmpty()) continue;

            // Check if we already nudged this specific cart recently
            String trackerKey = NUDGE_TRACKER_KEY + biz.getId() + ":" + phone;
            if (Boolean.TRUE.equals(redisTemplate.hasKey(trackerKey))) {
                continue; // Already nudged
            }

            // Logic: Is "abandoned"? (No activity for 30 minutes)
            Long ttlSeconds = redisTemplate.getExpire("cart:" + biz.getId() + ":" + phone, TimeUnit.SECONDS);
            
            // If < 5 mins remain of a 30 min session, it's very idle.
            if (ttlSeconds != null && ttlSeconds > 0 && ttlSeconds < 300) { // < 5 mins left
                sendRecoveryNudge(biz, phone, cart, settings);
                // Mark as nudged for 24 hours to avoid spamming
                redisTemplate.opsForValue().set(trackerKey, "true", 24, TimeUnit.HOURS);
            }
        }
    }

    private void sendRecoveryNudge(Business biz, String phone, Map<String, CartItem> cart, AISettings settings) {
        log.info("Sending cart recovery nudge to {} for business {}", phone, biz.getName());

        BigDecimal total = BigDecimal.ZERO;
        StringBuilder items = new StringBuilder();
        for (CartItem item : cart.values()) {
            total = total.add(item.getUnitPriceSnapshot().multiply(BigDecimal.valueOf(item.getQuantity())));
            items.append("- ").append(item.getProductName()).append("\n");
        }

        // --- EXPERT CHECK: Find best offer to close the sale ---
        String promoOffer = "";
        try {
            var customer = customerRepository.findByBusinessIdAndPhone(biz.getId(), phone).orElse(null);
            if (customer != null) {
                var best = promotionService.findBestOffer(biz.getId(), phone, total, cart, customer);
                if (best.applied()) {
                    promoOffer = "\n\n✨ *SPECIAL OFFER:* Use code *" + best.promo().getCode() + "* to get a discount on this order!";
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch recovery promo: {}", e.getMessage());
        }

        String nudgeMessage = "Hi! We noticed you left some items in your cart at *" + biz.getName() + "*:\n\n" +
            items.toString() + "\n" +
            "Total: *₹" + total + "*" + promoOffer + "\n\n" +
            "Would you like to complete your order? Just reply 'Confirm' and I'll settle it for you! 🛍️";

        whatsAppSender.sendText(phone, nudgeMessage, biz.getPhoneNumberId());
    }
}
