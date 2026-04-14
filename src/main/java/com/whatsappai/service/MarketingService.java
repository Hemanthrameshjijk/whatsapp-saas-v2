package com.whatsappai.service;

import com.whatsappai.entity.*;
import com.whatsappai.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class MarketingService {

    private final CartService cartService;
    private final CustomerService customerService;
    private final PromotionService promotionService;
    private final WhatsAppSender whatsAppSender;
    private final MarketingCampaignRepository campaignRepository;
    private final OrderRepository orderRepository;
    private final BusinessRepository businessRepository;
    private final CampaignLinkRepository linkRepository;
    private final ProductRepository productRepository;

    private static final java.util.regex.Pattern URL_PATTERN = 
        java.util.regex.Pattern.compile("https?://\\S+");


    public List<Map<String, Object>> getAbandonedCarts(UUID bizId) {
        var carts = cartService.getActiveCarts(bizId);
        List<Map<String, Object>> result = new ArrayList<>();

        for (var entry : carts.entrySet()) {
            String phone = entry.getKey();
            var cartItems = entry.getValue();
            if (cartItems.isEmpty()) continue;

            Optional<Customer> customer = customerService.find(bizId, phone);
            BigDecimal total = cartItems.values().stream()
                .map(ci -> ci.getUnitPriceSnapshot().multiply(BigDecimal.valueOf(ci.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Calculate best nudge
            PromotionService.DiscountResult nudge = PromotionService.DiscountResult.none();
            if (customer.isPresent()) {
                nudge = promotionService.findBestOffer(bizId, phone, total, cartItems, customer.get());
            }

            result.add(Map.of(
                "phone", phone,
                "name", customer.map(Customer::getName).orElse("Unknown"),
                "items", cartItems.values(),
                "total", total,
                "suggestedNudge", nudge.applied() ? (nudge.promo().getOfferLabel() != null ? nudge.promo().getOfferLabel() : nudge.promo().getCode()) : null,
                "nudgePromoId", nudge.applied() ? nudge.promo().getId().toString() : null
            ));
        }
        return result;
    }

    @Transactional
    public MarketingCampaign launchBroadcast(UUID bizId, String message, String audience, UUID promoId) {
        MarketingCampaign campaign = MarketingCampaign.builder()
            .businessId(bizId)
            .message(message)
            .audience(audience)
            .promoId(promoId)
            .sentCount(0)
            .build();
        
        MarketingCampaign saved = campaignRepository.save(campaign);
        
        // Trigger async sending
        sendBroadcastAsync(bizId, saved.getId(), message, audience, promoId);
        
        return saved;
    }

    @Async
    public void sendBroadcastAsync(UUID bizId, UUID campaignId, String messageTemplate, String audience, UUID promoId) {
        log.info("Starting async broadcast campaign: {}", campaignId);
        Business biz = businessRepository.findById(bizId).orElseThrow();
        
        String promoSuffix = "";
        if (promoId != null) {
            Promotion p = promotionService.findAll(bizId, Pageable.unpaged()).getContent().stream()
                .filter(x -> x.getId().equals(promoId)).findFirst().orElse(null);
            if (p != null) {
                promoSuffix = "\n\nUse code *" + p.getCode() + "* to get " + 
                              ("PERCENTAGE".equals(p.getDiscountType()) ? p.getDiscountValue() + "%" : "₹" + p.getDiscountValue()) + " OFF!";
            }
        }

        List<Customer> target;
        if ("VIPS".equals(audience)) {
            target = customerService.findAll(bizId, Pageable.unpaged()).getContent().stream()
                .filter(c -> !c.isBlocked())
                .filter(c -> isVIP(bizId, c.getPhone()))
                .toList();
        } else {
            target = customerService.findAll(bizId, Pageable.unpaged()).getContent().stream()
                .filter(c -> !c.isBlocked()).toList();
        }

        int sent = 0;
        for (Customer c : target) {
            try {
                String baseMsg = messageTemplate.replace("{name}", c.getName() != null ? c.getName() : "Customer") + promoSuffix;
                String trackedMsg = wrapLinks(bizId, campaignId, baseMsg, c.getPhone());
                
                whatsAppSender.sendText(c.getPhone(), trackedMsg, biz.getPhoneNumberId());
                sent++;
                Thread.sleep(300);
            } catch (Exception e) {
                log.error("Failed to send broadcast msg to {}: {}", c.getPhone(), e.getMessage());
            }
        }

        // Update campaign with final count
        int finalSent = sent;
        campaignRepository.findById(campaignId).ifPresent(camp -> {
            camp.setSentCount(finalSent);
            campaignRepository.save(camp);
        });
        
        log.info("Broadcast campaign {} completed. Sent: {}", campaignId, sent);
    }

    private boolean isVIP(UUID bizId, String phone) {
        BigDecimal spent = orderRepository.findByBusinessIdAndCustomerPhoneOrderByCreatedAtDesc(bizId, phone).stream()
            .filter(o -> List.of("CONFIRMED", "DELIVERED").contains(o.getStatus()))
            .map(Order::getTotalAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        return spent.compareTo(new BigDecimal("1000")) >= 0;
    }

    public Page<MarketingCampaign> getHistory(UUID bizId, Pageable pageable) {
        return campaignRepository.findByBusinessIdOrderByCreatedAtDesc(bizId, pageable);
    }

    public String createManualLink(UUID bizId, String name, UUID productId, String customUrl, String customCode) {
        String finalUrl = customUrl;
        
        if (productId != null) {
            Product product = productRepository.findById(productId).orElseThrow();
            Business biz = businessRepository.findById(bizId).orElseThrow();
            String msg = "Hi! I'm interested in buying " + product.getName() + " (Ref: " + name + ")";
            finalUrl = "https://wa.me/" + biz.getWhatsappNumber() + "?text=" + java.net.URLEncoder.encode(msg, java.nio.charset.StandardCharsets.UTF_8);
        }

        String shortCode = (customCode != null && !customCode.isBlank()) ? customCode : UUID.randomUUID().toString().substring(0, 8);
        
        CampaignLink link = CampaignLink.builder()
            .campaignId(null)
            .businessId(bizId)
            .influencerName(name)
            .productId(productId)
            .targetUrl(customUrl)
            .originalUrl(finalUrl)
            .shortCode(shortCode)
            .clickCount(0)
            .build();
            
        linkRepository.save(link);
        return shortCode;
    }

    public List<CampaignLink> getManualLinks(UUID bizId) {
        return linkRepository.findByBusinessId(bizId).stream()
            .filter(l -> l.getCampaignId() == null)
            .filter(l -> l.getInfluencerName() != null)
            .toList();
    }

    private String wrapLinks(UUID bizId, UUID campaignId, String text, String phone) {
        java.util.regex.Matcher matcher = URL_PATTERN.matcher(text);
        StringBuilder sb = new StringBuilder();
        int lastEnd = 0;

        while (matcher.find()) {
            sb.append(text, lastEnd, matcher.start());
            String originalUrl = matcher.group();
            
            // Generate or reuse link entry
            String shortCode = UUID.randomUUID().toString().substring(0, 8);
            CampaignLink link = CampaignLink.builder()
                .campaignId(campaignId)
                .businessId(bizId)
                .originalUrl(originalUrl)
                .shortCode(shortCode)
                .clickCount(0)
                .build();
            linkRepository.save(link);

            // TODO: In production, fetch this from business config or app properties
            String baseUrl = "https://ai.whatsappstore.com/l/"; 
            sb.append(baseUrl).append(shortCode).append("?p=").append(phone);
            
            lastEnd = matcher.end();
        }
        sb.append(text.substring(lastEnd));
        return sb.toString();
    }
}
