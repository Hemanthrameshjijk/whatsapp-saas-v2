package com.whatsappai.controller;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.whatsappai.entity.AISettings;
import com.whatsappai.entity.Business;
import com.whatsappai.entity.CampaignLink;
import com.whatsappai.entity.ChatMessage;
import com.whatsappai.entity.Customer;
import com.whatsappai.entity.MarketingCampaign;
import com.whatsappai.entity.Order;
import com.whatsappai.entity.Product;
import com.whatsappai.entity.Promotion;
import com.whatsappai.entity.SupportTicket;
import com.whatsappai.model.ChatSummaryProjection;
import com.whatsappai.repository.AISettingsRepository;
import com.whatsappai.repository.BusinessRepository;
import com.whatsappai.repository.CampaignLinkRepository;
import com.whatsappai.repository.ChatMessageRepository;
import com.whatsappai.repository.MarketingCampaignRepository;
import com.whatsappai.repository.OrderRepository;
import com.whatsappai.security.JwtService;
import com.whatsappai.service.CartService;
import com.whatsappai.service.ChatPersistenceService;
import com.whatsappai.service.CustomerService;
import com.whatsappai.service.MarketingService;
import com.whatsappai.service.NotificationService;
import com.whatsappai.service.OrderService;
import com.whatsappai.service.ProductService;
import com.whatsappai.service.PromotionService;
import com.whatsappai.service.SupportTicketService;
import com.whatsappai.service.WhatsAppSender;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/dashboard")
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
@RequiredArgsConstructor
public class DashboardController {

    private final JwtService jwtService;
    private final ProductService productService;
    private final OrderService orderService;
    private final CustomerService customerService;
    private final NotificationService notificationService;
    private final ChatMessageRepository chatMessageRepository;
    private final AISettingsRepository aiSettingsRepository;
    private final OrderRepository orderRepository;
    private final SupportTicketService supportTicketService;
    private final PromotionService promotionService;
    private final CartService cartService;
    private final BusinessRepository businessRepository;
    private final WhatsAppSender whatsAppSender;
    private final MarketingService marketingService;
    private final MarketingCampaignRepository campaignRepository;
    private final CampaignLinkRepository linkRepository;
    private final ChatPersistenceService chatPersistenceService;



    // ── Conversations ──────────────────────────────────────────
    @GetMapping("/conversations")
    public ResponseEntity<Page<ChatMessage>> getConversations(
            Authentication auth, Pageable pageable,
            @RequestParam(required = false) String phone) {
        UUID biz = jwtService.extractBusinessId(auth);
        if (phone != null && !phone.isBlank()) {
            return ResponseEntity.ok(chatMessageRepository
                .findByBusinessIdAndCustomerPhoneOrderByCreatedAtDesc(biz, phone.trim(), pageable));
        }
        return ResponseEntity.ok(chatMessageRepository.findByBusinessId(biz, pageable));
    }

    @GetMapping("/conversations/recent")
    public ResponseEntity<List<ChatSummaryProjection>> getRecentConversations(Authentication auth) {
        UUID biz = jwtService.extractBusinessId(auth);
        return ResponseEntity.ok(chatMessageRepository.findRecentChatSummaries(biz));
    }

    // ── Orders ─────────────────────────────────────────────────
    @GetMapping("/orders")
    public ResponseEntity<Page<Order>> getOrders(
            Authentication auth, Pageable pageable,
            @RequestParam(required = false) String status) {
        UUID biz = jwtService.extractBusinessId(auth);
        if (status != null) return ResponseEntity.ok(orderService.findByStatus(biz, status, pageable));
        return ResponseEntity.ok(orderService.findAll(biz, pageable));
    }

    @PatchMapping("/orders/{id}/status")
    public ResponseEntity<Void> updateOrderStatus(
            Authentication auth, @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        UUID biz = jwtService.extractBusinessId(auth);
        orderService.findByIdAndBusiness(id, biz)
            .orElseThrow(() -> new com.whatsappai.exception.OrderNotFoundException(id));
        orderService.updateStatus(id, biz, body.get("status"));
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/orders/{id}/payment")
    public ResponseEntity<Void> markPaid(Authentication auth, @PathVariable UUID id) {
        UUID biz = jwtService.extractBusinessId(auth);
        orderService.findByIdAndBusiness(id, biz)
            .orElseThrow(() -> new com.whatsappai.exception.OrderNotFoundException(id));
        orderService.markPaid(id, biz);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/orders/{id}/address")
    public ResponseEntity<Void> updateOrderAddress(
            Authentication auth, @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        UUID biz = jwtService.extractBusinessId(auth);
        orderService.findByIdAndBusiness(id, biz)
            .orElseThrow(() -> new com.whatsappai.exception.OrderNotFoundException(id));
        orderService.updateAddress(id, biz, body.get("address"));
        return ResponseEntity.ok().build();
    }

    // ── Products ───────────────────────────────────────────────
    @GetMapping("/products")
    public ResponseEntity<Page<Product>> getProducts(Authentication auth, Pageable pageable) {
        UUID biz = jwtService.extractBusinessId(auth);
        return ResponseEntity.ok(productService.findAll(biz, pageable));
    }

    @PostMapping("/products")
    public ResponseEntity<Product> createProduct(
            Authentication auth, @RequestBody Map<String, Object> body) {
        UUID biz = jwtService.extractBusinessId(auth);
        Product p = Product.builder()
            .businessId(biz)
            .name((String) body.get("name"))
            .description((String) body.get("description"))
            .price(new BigDecimal(body.get("price").toString()))
            .category((String) body.get("category"))
            .sku((String) body.get("sku"))
            .brand((String) body.get("brand"))
            .type((String) body.get("type"))
            .color((String) body.get("color"))
            .size((String) body.get("size"))
            .outOfStock(body.containsKey("outOfStock") ? Boolean.parseBoolean(body.get("outOfStock").toString()) : false)
            .warrantyMode((String) body.getOrDefault("warrantyMode", "GLOBAL"))
            .warrantyDetails((String) body.get("warrantyDetails"))
            .warrantyClaimRules((String) body.get("warrantyClaimRules"))
            .returnMode((String) body.getOrDefault("returnMode", "GLOBAL"))
            .customReturnPolicy((String) body.get("customReturnPolicy"))
            .stockQty(body.containsKey("stockQty") ? Integer.parseInt(body.get("stockQty").toString()) : 0)
            .photoUrl((String) body.get("photoUrl"))
            .build();
        return ResponseEntity.ok(productService.save(p));
    }

    @PutMapping("/products/{id}")
    public ResponseEntity<Product> updateProduct(
            Authentication auth, @PathVariable UUID id,
            @RequestBody Map<String, Object> body) {
        UUID biz = jwtService.extractBusinessId(auth);
        Product p = productService.findByIdAndBusiness(id, biz)
            .orElseThrow(() -> new com.whatsappai.exception.ProductNotFoundException(id));
        if (body.containsKey("name")) p.setName((String) body.get("name"));
        if (body.containsKey("description")) p.setDescription((String) body.get("description"));
        if (body.containsKey("price")) p.setPrice(new BigDecimal(body.get("price").toString()));
        if (body.containsKey("category")) p.setCategory((String) body.get("category"));
        if (body.containsKey("sku")) p.setSku((String) body.get("sku"));
        if (body.containsKey("brand")) p.setBrand((String) body.get("brand"));
        if (body.containsKey("type")) p.setType((String) body.get("type"));
        if (body.containsKey("color")) p.setColor((String) body.get("color"));
        if (body.containsKey("size")) p.setSize((String) body.get("size"));
        if (body.containsKey("outOfStock")) p.setOutOfStock(Boolean.parseBoolean(body.get("outOfStock").toString()));
        if (body.containsKey("warrantyMode")) p.setWarrantyMode((String) body.get("warrantyMode"));
        if (body.containsKey("warrantyDetails")) p.setWarrantyDetails((String) body.get("warrantyDetails"));
        if (body.containsKey("warrantyClaimRules")) p.setWarrantyClaimRules((String) body.get("warrantyClaimRules"));
        if (body.containsKey("returnMode")) p.setReturnMode((String) body.get("returnMode"));
        if (body.containsKey("customReturnPolicy")) p.setCustomReturnPolicy((String) body.get("customReturnPolicy"));
        if (body.containsKey("stockQty")) p.setStockQty(Integer.parseInt(body.get("stockQty").toString()));
        if (body.containsKey("photoUrl")) p.setPhotoUrl((String) body.get("photoUrl"));
        return ResponseEntity.ok(productService.save(p));
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<Void> deleteProduct(Authentication auth, @PathVariable UUID id) {
        UUID biz = jwtService.extractBusinessId(auth);
        productService.softDelete(id, biz);
        return ResponseEntity.ok().build();
    }

    // ── Settings ───────────────────────────────────────────────
    @GetMapping("/settings")
    public ResponseEntity<AISettings> getSettings(Authentication auth) {
        UUID biz = jwtService.extractBusinessId(auth);
        return ResponseEntity.ok(aiSettingsRepository.findByBusinessId(biz).orElseThrow());
    }

    @PutMapping("/settings")
    public ResponseEntity<AISettings> updateSettings(
            Authentication auth, @RequestBody Map<String, Object> body) {
        UUID biz = jwtService.extractBusinessId(auth);
        AISettings s = aiSettingsRepository.findByBusinessId(biz).orElseThrow();
        if (body.containsKey("toneStyle")) s.setToneStyle((String) body.get("toneStyle"));
        if (body.containsKey("languageStyle")) s.setLanguageStyle((String) body.get("languageStyle"));
        if (body.containsKey("upiId")) s.setUpiId((String) body.get("upiId"));
        if (body.containsKey("deliveryCharge")) s.setDeliveryCharge(new BigDecimal(body.get("deliveryCharge").toString()));
        if (body.containsKey("freeDeliveryAbove")) s.setFreeDeliveryAbove(new BigDecimal(body.get("freeDeliveryAbove").toString()));
        if (body.containsKey("competitorKeywords")) s.setCompetitorKeywords((String) body.get("competitorKeywords"));
        if (body.containsKey("guardrailAllowlist")) s.setGuardrailAllowlist((String) body.get("guardrailAllowlist"));
        if (body.containsKey("greetingTemplate")) s.setGreetingTemplate((String) body.get("greetingTemplate"));
        if (body.containsKey("shopAddress")) s.setShopAddress((String) body.get("shopAddress"));
        if (body.containsKey("globalReturnPolicy")) s.setGlobalReturnPolicy((String) body.get("globalReturnPolicy"));
        if (body.containsKey("globalReturnMode")) s.setGlobalReturnMode((String) body.get("globalReturnMode"));
        if (body.containsKey("globalWarrantyDetails")) s.setGlobalWarrantyDetails((String) body.get("globalWarrantyDetails"));
        if (body.containsKey("globalWarrantyClaimRules")) s.setGlobalWarrantyClaimRules((String) body.get("globalWarrantyClaimRules"));
        if (body.containsKey("globalWarrantyMode")) s.setGlobalWarrantyMode((String) body.get("globalWarrantyMode"));
        if (body.containsKey("generalFaq")) s.setGeneralFaq((String) body.get("generalFaq"));
        s.setUpdatedAt(java.time.LocalDateTime.now());
        return ResponseEntity.ok(aiSettingsRepository.save(s));
    }

    // ── Guardrail Log ──────────────────────────────────────────
    @GetMapping("/guardrail-log")
    public ResponseEntity<Page<ChatMessage>> getGuardrailLog(
            Authentication auth, Pageable pageable,
            @RequestParam(required = false) String reason) {
        UUID biz = jwtService.extractBusinessId(auth);
        if (reason != null) return ResponseEntity.ok(chatMessageRepository.findGuardrailTriggersByReason(biz, reason, pageable));
        return ResponseEntity.ok(chatMessageRepository.findGuardrailTriggers(biz, pageable));
    }

    // ── Customers ──────────────────────────────────────────────
    @GetMapping("/customers")
    public ResponseEntity<Page<Customer>> getCustomers(Authentication auth, Pageable pageable) {
        UUID biz = jwtService.extractBusinessId(auth);
        return ResponseEntity.ok(customerService.findAll(biz, pageable));
    }

    @PatchMapping("/customers/{phone}/block")
    public ResponseEntity<Void> blockCustomer(
            Authentication auth, @PathVariable String phone,
            @RequestBody Map<String, Boolean> body) {
        UUID biz = jwtService.extractBusinessId(auth);
        customerService.setBlocked(biz, phone, Boolean.TRUE.equals(body.get("blocked")));
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/customers/{phone}/handled")
    public ResponseEntity<Void> markCustomerHandled(Authentication auth, @PathVariable String phone) {
        UUID biz = jwtService.extractBusinessId(auth);
        customerService.setRequiresHuman(biz, phone, false);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/customers/{phone}/details")
    public ResponseEntity<Map<String, Object>> getCustomerDetails(
            Authentication auth, @PathVariable String phone) {
        UUID biz = jwtService.extractBusinessId(auth);
        Customer customer = customerService.find(biz, phone)
            .orElseThrow(() -> new RuntimeException("Customer not found"));

        List<Order> orders = orderRepository.findByBusinessIdAndCustomerPhoneOrderByCreatedAtDesc(biz, phone);
        List<SupportTicket> tickets = supportTicketService.findByPhone(biz, phone);

        BigDecimal totalSpent = orders.stream()
            .filter(o -> List.of("CONFIRMED", "DELIVERED").contains(o.getStatus()))
            .map(Order::getTotalAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return ResponseEntity.ok(Map.of(
            "customer", customer,
            "orders", orders,
            "tickets", tickets,
            "totalSpent", totalSpent
        ));
    }

    // ── SSE ────────────────────────────────────────────────────
    @GetMapping(value = "/sse/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamEvents(@RequestParam("token") String token) {
        // Validate token and extract businessId
        var claims = jwtService.validateAndExtract(token);
        String businessIdStr = claims.get("businessId", String.class);
        UUID biz = UUID.fromString(businessIdStr);
        return notificationService.registerEmitter(biz);
    }

    // ── Analytics ──────────────────────────────────────────────
    @GetMapping("/analytics")
    public ResponseEntity<Map<String, Object>> getAnalytics(Authentication auth) {
        UUID biz = jwtService.extractBusinessId(auth);
        long guardrailCount = chatMessageRepository.countGuardrailTriggers(biz);
        // ALWAYS filter by business_id — invariant #2
        long orders30d = orderRepository.countByBusinessIdAndDays(biz, 30);
        long orders7d = orderRepository.countByBusinessIdAndDays(biz, 7);
        long orders1d = orderRepository.countByBusinessIdAndDays(biz, 1);
        java.math.BigDecimal revenue30d = orderRepository.sumRevenueByDays(biz, 30);
        java.math.BigDecimal revenue7d = orderRepository.sumRevenueByDays(biz, 7);
        return ResponseEntity.ok(Map.of(
            "guardrailTriggers", guardrailCount,
            "orders30d", orders30d,
            "orders7d", orders7d,
            "orders1d", orders1d,
            "revenue30d", revenue30d,
            "revenue7d", revenue7d,
            "businessId", biz
        ));
    }

    // ── Support Tickets ───────────────────────────────────────
    @GetMapping("/tickets")
    public ResponseEntity<?> getTickets(
            Authentication auth, Pageable pageable,
            @RequestParam(required = false) String status) {
        UUID biz = jwtService.extractBusinessId(auth);
        if (status != null && !status.isBlank()) {
            return ResponseEntity.ok(supportTicketService.findByStatus(biz, status, pageable));
        }
        return ResponseEntity.ok(supportTicketService.findAll(biz, pageable));
    }

    @GetMapping("/tickets/stats")
    public ResponseEntity<?> getTicketStats(Authentication auth) {
        UUID biz = jwtService.extractBusinessId(auth);
        return ResponseEntity.ok(Map.of("openTickets", supportTicketService.countOpen(biz)));
    }

    @PatchMapping("/tickets/{id}/status")
    public ResponseEntity<Void> updateTicketStatus(
            Authentication auth, @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        UUID biz = jwtService.extractBusinessId(auth);
        supportTicketService.updateStatus(id, biz, body.get("status"), body.get("adminNotes"));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/tickets")
    public ResponseEntity<?> createTicketManual(
            Authentication auth, @RequestBody Map<String, String> body) {
        UUID biz = jwtService.extractBusinessId(auth);
        String phone = customerService.normalisePhone(body.get("phone"));
        UUID productId = UUID.fromString(body.get("productId"));
        UUID orderId = null; 
        if (body.get("orderId") != null && !body.get("orderId").isBlank()) {
            orderId = UUID.fromString(body.get("orderId"));
        }
        
        Customer customer = customerService.find(biz, phone).orElseGet(() -> customerService.findOrCreate(biz, phone));

        SupportTicketService.TicketResult result = supportTicketService.createTicket(
            biz, customer.getId(), phone, productId, orderId, body.get("type"), body.get("reason")
        );

        if (!result.ok()) return ResponseEntity.badRequest().body(Map.of("error", result.message()));
        return ResponseEntity.ok(result.ticket());
    }

    // ── Promotions ───────────────────────────────────────────
    @GetMapping("/promotions")
    public ResponseEntity<?> getPromotions(Authentication auth, Pageable pageable) {
        UUID biz = jwtService.extractBusinessId(auth);
        return ResponseEntity.ok(promotionService.findAll(biz, pageable));
    }

    @PostMapping("/promotions")
    public ResponseEntity<?> createPromotion(Authentication auth,
            @RequestBody com.whatsappai.entity.Promotion promo) {
        UUID biz = jwtService.extractBusinessId(auth);
        return ResponseEntity.ok(promotionService.create(biz, promo));
    }

    // ── Marketing ──────────────────────────────────────────────
    @GetMapping("/marketing/abandoned-carts")
    public ResponseEntity<List<Map<String, Object>>> getAbandonedCarts(Authentication auth) {
        UUID biz = jwtService.extractBusinessId(auth);
        return ResponseEntity.ok(marketingService.getAbandonedCarts(biz));
    }

    @GetMapping("/marketing/campaigns")
    public ResponseEntity<Page<Map<String, Object>>> getCampaignHistory(Authentication auth, Pageable pageable) {
        UUID biz = jwtService.extractBusinessId(auth);
        Page<MarketingCampaign> history = marketingService.getHistory(biz, pageable);
        
        Page<Map<String, Object>> result = history.map(camp -> {
            List<CampaignLink> links = linkRepository.findByCampaignId(camp.getId());
            int totalClicks = links.stream().mapToInt(CampaignLink::getClickCount).sum();
            
            Map<String, Object> map = new HashMap<>();
            map.put("id", camp.getId());
            map.put("message", camp.getMessage());
            map.put("audience", camp.getAudience());
            map.put("sentCount", camp.getSentCount());
            map.put("createdAt", camp.getCreatedAt());
            map.put("clickCount", totalClicks);
            return map;
        });
        
        return ResponseEntity.ok(result);
    }

    @PostMapping("/marketing/remind")
    public ResponseEntity<Void> sendCartReminder(
            Authentication auth, @RequestBody Map<String, String> body) {
        UUID bizId = jwtService.extractBusinessId(auth);
        String phone = body.get("phone");
        String promoId = body.get("promoId");

        Business biz = businessRepository.findById(bizId).orElseThrow();
        var cart = cartService.getCart(bizId, phone);
        if (cart.isEmpty()) return ResponseEntity.badRequest().build();

        AISettings settings = aiSettingsRepository.findByBusinessId(bizId).orElseThrow();
        String cartText = cartService.formatCart(cart, settings);
        
        String incentive = "";
        if (promoId != null && !promoId.isBlank()) {
            Promotion promo = promotionService.findAll(bizId, Pageable.unpaged()).getContent().stream()
                .filter(p -> p.getId().toString().equals(promoId)).findFirst().orElse(null);
            if (promo != null) {
                incentive = "\n\n🎁 *Special Gift for you:* Use code *" + promo.getCode() + "* to get " + 
                            ("PERCENTAGE".equals(promo.getDiscountType()) ? promo.getDiscountValue() + "%" : "₹" + promo.getDiscountValue()) + " OFF!";
            }
        }

        String reminder = "👋 Hi! We noticed you left some items in your cart:\n\n" + 
                          cartText + incentive + "\n\nWould you like to complete your order? Type 'confirm' to checkout! 🛍️";
        
        whatsAppSender.sendText(phone, reminder, biz.getPhoneNumberId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/marketing/broadcast")
    public ResponseEntity<Map<String, Object>> sendBroadcast(
            Authentication auth, @RequestBody Map<String, String> body) {
        UUID bizId = jwtService.extractBusinessId(auth);
        String message = body.get("message");
        String audience = body.getOrDefault("audience", "ALL");
        UUID promoId = null;
        if (body.get("promoId") != null && !body.get("promoId").isBlank()) {
            promoId = UUID.fromString(body.get("promoId"));
        }

        MarketingCampaign campaign = marketingService.launchBroadcast(bizId, message, audience, promoId);
        return ResponseEntity.ok(Map.of("campaignId", campaign.getId(), "status", "QUEUED"));
    }


    @PutMapping("/promotions/{id}")
    public ResponseEntity<?> updatePromotion(Authentication auth, @PathVariable UUID id,
            @RequestBody com.whatsappai.entity.Promotion promo) {
        UUID biz = jwtService.extractBusinessId(auth);
        return ResponseEntity.ok(promotionService.update(id, biz, promo));
    }

    @DeleteMapping("/promotions/{id}")
    public ResponseEntity<Void> deletePromotion(Authentication auth, @PathVariable UUID id) {
        UUID biz = jwtService.extractBusinessId(auth);
        promotionService.delete(id, biz);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/marketing/links/manual")
    public ResponseEntity<String> createManualLink(Authentication auth, @RequestBody Map<String, String> request) {
        UUID biz = jwtService.extractBusinessId(auth);
        String name = request.get("name");
        String productIdStr = request.get("productId");
        UUID productId = (productIdStr != null && !productIdStr.isEmpty()) ? UUID.fromString(productIdStr) : null;
        String targetUrl = request.get("targetUrl");
        String customCode = request.get("customCode");
        
        String code = marketingService.createManualLink(biz, name, productId, targetUrl, customCode);
        return ResponseEntity.ok(code);
    }

    @GetMapping("/marketing/links/influencers")
    public ResponseEntity<List<Map<String, Object>>> getInfluencerLinks(Authentication auth) {
        UUID biz = jwtService.extractBusinessId(auth);
        List<CampaignLink> links = marketingService.getManualLinks(biz);
        
        List<Map<String, Object>> result = links.stream().map(link -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", link.getId());
            map.put("influencerName", link.getInfluencerName());
            map.put("shortCode", link.getShortCode());
            map.put("clickCount", link.getClickCount());
            map.put("createdAt", link.getCreatedAt());
            map.put("productId", link.getProductId());
            
            // Calculate acquired customers
            long acquired = customerService.findAll(biz, Pageable.unpaged()).getContent().stream()
                .filter(c -> link.getInfluencerName().equals(c.getReferredBy()))
                .count();
            map.put("acquiredCount", acquired);
            
            return map;
        }).toList();
        
        return ResponseEntity.ok(result);
    }

    @PostMapping("/conversations/{phone}/reply")
    public ResponseEntity<Void> manualReply(Authentication auth, @PathVariable String phone, @RequestBody Map<String, String> request) {
        UUID bizId = jwtService.extractBusinessId(auth);
        String message = request.get("message");
        
        Business biz = businessRepository.findById(bizId).orElseThrow();
        whatsAppSender.sendText(phone, message, biz.getPhoneNumberId());
        
        // Persist the manual reply
        chatPersistenceService.persistMessage(bizId, phone, null, message, "MANUAL-" + UUID.randomUUID(), false, null);
        
        // Auto-switch to human mode if not already
        customerService.setRequiresHuman(bizId, phone, true);
        
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/customers/{phone}/handoff")
    public ResponseEntity<Void> toggleHandoff(Authentication auth, @PathVariable String phone, @RequestBody Map<String, Boolean> request) {
        UUID bizId = jwtService.extractBusinessId(auth);
        boolean requiresHuman = request.getOrDefault("requiresHuman", true);
        customerService.setRequiresHuman(bizId, phone, requiresHuman);
        return ResponseEntity.ok().build();
    }
}
