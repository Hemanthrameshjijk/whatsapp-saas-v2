package com.whatsappai.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.whatsappai.entity.AISettings;
import com.whatsappai.entity.Order;
import com.whatsappai.entity.OrderItem;
import com.whatsappai.entity.Product;
import com.whatsappai.exception.ProductNotFoundException;
import com.whatsappai.model.CartItem;
import com.whatsappai.model.ConversationSession;
import com.whatsappai.model.ExecutionContext;
import com.whatsappai.model.FunctionResult;
import com.whatsappai.repository.AISettingsRepository;
import com.whatsappai.repository.ProductRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class FunctionExecutor {

    private final CartService cartService;
    private final ProductRepository productRepository;
    private final AISettingsRepository aiSettingsRepository;
    private final OrderService orderService;
    private final NotificationService notificationService;
    private final CustomerService customerService;
    private final SupportTicketService supportTicketService;
    private final PromotionService promotionService;
    private final WhatsAppSender whatsAppSender;

    @Transactional
    public FunctionResult execute(String toolName, Map<String, Object> args,
                                   ExecutionContext ctx, ConversationSession session) {
        return switch (toolName) {
            case "add_to_cart"        -> addToCart(args, ctx, session);
            case "remove_from_cart"   -> removeFromCart(args, ctx);
            case "view_cart"          -> viewCart(ctx);
            case "confirm_order"      -> confirmOrder(args, ctx, session);
            case "quick_buy"          -> quickBuy(args, ctx, session);
            case "cancel_order"       -> cancelOrder(args, ctx);
            case "check_order_status" -> checkOrderStatus(args, ctx);
            case "repeat_last_order"  -> repeatLastOrder(ctx);
            case "request_human_agent"-> requestHumanAgent(args, ctx);
            case "raise_support_ticket"-> raiseSupportTicket(args, ctx);
            case "check_ticket_status"-> checkTicketStatus(ctx);
            case "list_promotions"    -> listPromotions(ctx);
            case "request_variant_selection" -> requestVariantSelection(args, ctx);
            default -> FunctionResult.error("Unknown tool: " + toolName);
        };
    }

    /** 
     * QUICK BUY: Bypasses the cart for a single item.
     * Combines logic of addToCart and confirmOrder for speed.
     */
    @Transactional
    public FunctionResult quickBuy(Map<String, Object> args, ExecutionContext ctx, ConversationSession session) {
        try {
            UUID productId = UUID.fromString(args.get("product_id").toString());
            int qty = Integer.parseInt(args.getOrDefault("quantity", "1").toString());
            String address = (String) args.get("delivery_address");

            Product product = productRepository.findByIdAndBusinessId(productId, ctx.getBiz())
                .orElseThrow(() -> new ProductNotFoundException(productId));

            if (product.getStockQty() < qty) {
                return FunctionResult.error("Sorry, only " + product.getStockQty() + " available for " + product.getName());
            }

            // Create temporary cart item list for common order logic
            List<OrderItem> items = new ArrayList<>();
            BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(qty));
            items.add(OrderItem.builder()
                .productId(product.getId())
                .productName(product.getName())
                .quantity(qty)
                .unitPrice(product.getPrice())
                .subtotal(subtotal)
                .build());

            AISettings ai = aiSettingsRepository.findByBusinessId(ctx.getBiz()).orElseThrow();
            BigDecimal delivery = cartService.calcDeliveryCharge(subtotal, ai);

            // Promo calculation
            String promoCode = (String) args.get("promo_code");
            BigDecimal discount = BigDecimal.ZERO;
            if (promoCode != null && !promoCode.isBlank()) {
               com.whatsappai.entity.Customer customer = customerService.findOrCreate(ctx.getBiz(), ctx.getPhone());
               // Use singleton map for promo validation
               Map<String, CartItem> tempCart = Map.of(productId.toString(), CartItem.builder().productId(productId).quantity(qty).unitPriceSnapshot(product.getPrice()).build());
               PromotionService.DiscountResult dr = promotionService.validateAndApply(ctx.getBiz(), ctx.getPhone(), promoCode, subtotal, tempCart, customer);
               if (dr.applied()) {
                   discount = dr.discount();
               }
            }

            BigDecimal total = subtotal.add(delivery).subtract(discount);
            if (total.compareTo(BigDecimal.ZERO) < 0) total = BigDecimal.ZERO;

            if (address == null || address.isBlank()) address = session.getDeliveryAddress();
            if (address == null || address.isBlank()) {
                return FunctionResult.error("I need a delivery address to complete your quick order. Please send it first! 🙏");
            }

            ctx.setDeliveryAddress(address);
            Order order = orderService.createOrder(ctx, items, total, ai);
            order.setPaymentMethod((String) args.getOrDefault("payment_method", "COD"));

            // Persistent changes
            productRepository.decrementStock(productId, qty);
            customerService.updateAddress(ctx.getBiz(), ctx.getPhone(), address, null, null);
            notificationService.notifyOwnerNewOrder(order, ctx.getBiz());

            StringBuilder reply = new StringBuilder("⚡ *Quick Order Confirmed!*\n");
            reply.append("Product: ").append(product.getName()).append(" x").append(qty).append("\n");
            reply.append("Total: *₹").append(total).append("*\n");
            reply.append("Address: ").append(address).append("\n\n");
            reply.append("Order ID: ORDER-").append(order.getId().toString().substring(0, 8)).append("\n");
            reply.append("Thank you for your order! 🙏");

            return FunctionResult.success(reply.toString(), order);

        } catch (Exception e) {
            return FunctionResult.error("Quick buy failed: " + e.getMessage());
        }
    }

    private FunctionResult requestHumanAgent(Map<String, Object> args, ExecutionContext ctx) {
        String reason = args.getOrDefault("reason", "Customer requested human assistance.").toString();
        customerService.handoffToHuman(ctx.getBiz(), ctx.getPhone(), reason);
        
        AISettings settings = aiSettingsRepository.findByBusinessId(ctx.getBiz()).orElseThrow();
        if (settings.getOpenTime() != null && settings.getCloseTime() != null) {
            java.time.LocalTime now = java.time.LocalTime.now();
            if (now.isBefore(settings.getOpenTime()) || now.isAfter(settings.getCloseTime())) {
                return FunctionResult.success("I've flagged this for our human support team, but they are currently offline. They will get back to you as soon as they log in. 🙏");
            }
        }
        return FunctionResult.success("I've notified our support team and an agent will assist you shortly. 🙏");
    }

    private FunctionResult addToCart(Map<String, Object> args, ExecutionContext ctx, ConversationSession session) {
        try {
            UUID productId = UUID.fromString(args.get("product_id").toString());
            int qty = Integer.parseInt(args.get("quantity").toString());
            Product product = productRepository.findByIdAndBusinessId(productId, ctx.getBiz())
                .orElseThrow(() -> new ProductNotFoundException(productId));
            if (product.getStockQty() < qty) {
                List<Product> alts = findAlternatives(ctx.getBiz(), product.getCategory(), productId);
                StringBuilder msg = new StringBuilder("Sorry, only ").append(product.getStockQty())
                    .append(" units available for ").append(product.getName()).append(".");
                if (!alts.isEmpty()) {
                    msg.append(" You might also like: ");
                    alts.forEach(a -> msg.append(a.getName()).append(" (₹").append(a.getPrice()).append("), "));
                }
                return FunctionResult.error(msg.toString().trim().replaceAll(",$", ""));
            }
            CartItem item = CartItem.builder()
                .productId(productId).productName(product.getName())
                .quantity(qty).unitPriceSnapshot(product.getPrice())
                .brand(product.getBrand())
                .category(product.getCategory()).build();
            cartService.addItem(ctx.getBiz(), ctx.getPhone(), item);
            session.getCart().put(productId.toString(), item);
            return FunctionResult.success(product.getName() + " x" + qty + " added to cart! 🛒");
        } catch (Exception e) {
            return FunctionResult.error("Could not add item: " + e.getMessage());
        }
    }

    private FunctionResult removeFromCart(Map<String, Object> args, ExecutionContext ctx) {
        UUID productId = UUID.fromString(args.get("product_id").toString());
        cartService.removeItem(ctx.getBiz(), ctx.getPhone(), productId);
        return FunctionResult.success("Item removed from cart.");
    }

    private FunctionResult viewCart(ExecutionContext ctx) {
        AISettings settings = aiSettingsRepository.findByBusinessId(ctx.getBiz()).orElseThrow();
        Map<String, CartItem> cart = cartService.getCart(ctx.getBiz(), ctx.getPhone());
        if (cart.isEmpty()) {
            return FunctionResult.success("Your cart is empty. Browse our products and add items you like! Type 'show products' to see what we have. 🛍️");
        }
        return FunctionResult.success(cartService.formatCart(cart, settings));
    }

    /** CRITICAL: re-fetches ALL prices from PostgreSQL. Cart snapshot prices NEVER trusted. */
    @Transactional
    public FunctionResult confirmOrder(Map<String, Object> args, ExecutionContext ctx, ConversationSession session) {
        Map<String, CartItem> cart = cartService.getCart(ctx.getBiz(), ctx.getPhone());
        if (cart.isEmpty()) return FunctionResult.error("Your cart is empty. Add some products first!");

        BigDecimal total = BigDecimal.ZERO;
        List<OrderItem> items = new ArrayList<>();

        for (CartItem cartItem : cart.values()) {
            // ALWAYS re-fetch price from PostgreSQL — NEVER use cart snapshot price
            Product product = productRepository
                .findByIdAndBusinessId(cartItem.getProductId(), ctx.getBiz())
                .orElseThrow(() -> new ProductNotFoundException(cartItem.getProductId()));

            if (product.getStockQty() < cartItem.getQuantity())
                return FunctionResult.error("Stock insufficient for " + product.getName() +
                    ". Only " + product.getStockQty() + " available.");

            BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            total = total.add(subtotal);

            items.add(OrderItem.builder()
                .productId(product.getId())
                .productName(product.getName())
                .quantity(cartItem.getQuantity())
                .unitPrice(product.getPrice()) // live price from DB
                .subtotal(subtotal)
                .build());
        }

        AISettings ai = aiSettingsRepository.findByBusinessId(ctx.getBiz()).orElseThrow();
        BigDecimal delivery = cartService.calcDeliveryCharge(total, ai);

        // ── Promo Code Validation ────────────────────────────────
        String promoCode = args != null ? (String) args.get("promo_code") : null;
        BigDecimal discount = BigDecimal.ZERO;
        String appliedPromoCode = null;

        if (promoCode != null && !promoCode.isBlank()) {
            com.whatsappai.entity.Customer customer = customerService.findOrCreate(ctx.getBiz(), ctx.getPhone());
            PromotionService.DiscountResult promoResult = promotionService.validateAndApply(
                ctx.getBiz(), ctx.getPhone(), promoCode, total, cart, customer);

            if (promoResult.message() != null) {
                // Promo failed validation — return error to customer
                return FunctionResult.error(promoResult.message());
            }
            if (promoResult.applied()) {
                discount = promoResult.discount();
                appliedPromoCode = promoResult.promo().getCode();
            }
        } else {
            // No promo code — check for auto-apply offers
            com.whatsappai.entity.Customer customer = customerService.findOrCreate(ctx.getBiz(), ctx.getPhone());
            PromotionService.DiscountResult offerResult = promotionService.findBestOffer(
                ctx.getBiz(), ctx.getPhone(), total, cart, customer);
            if (offerResult.applied()) {
                discount = offerResult.discount();
                appliedPromoCode = offerResult.promo().getOfferLabel() != null
                    ? offerResult.promo().getOfferLabel()
                    : offerResult.promo().getCode();
            }
        }

        BigDecimal grandTotal = total.add(delivery).subtract(discount);
        if (grandTotal.compareTo(BigDecimal.ZERO) < 0) grandTotal = BigDecimal.ZERO;

        // Set payment/delivery from args if provided
        String paymentMethod = args != null ? (String) args.getOrDefault("payment_method", "COD") : "COD";
        // Fallback 1: Tool arguments (Preferred)
        String deliveryAddress = args != null ? (String) args.get("delivery_address") : null;
        Double lat = args != null && args.get("lat") != null ? Double.parseDouble(args.get("lat").toString()) : null;
        Double lng = args != null && args.get("lng") != null ? Double.parseDouble(args.get("lng").toString()) : null;

        // Fallback 2: Session (WhatsApp Location)
        if (deliveryAddress == null) deliveryAddress = session.getDeliveryAddress();
        if (lat == null) lat = session.getDeliveryLat();
        if (lng == null) lng = session.getDeliveryLng();

        // Fallback 3: Customer profile (Last used)
        if (deliveryAddress == null || deliveryAddress.isBlank()) {
            Optional<com.whatsappai.entity.Customer> cust = customerService.find(ctx.getBiz(), ctx.getPhone());
            if (cust.isPresent()) {
                deliveryAddress = cust.get().getLastDeliveryAddress();
                lat = cust.get().getLastLat();
                lng = cust.get().getLastLng();
            }
        }

        ctx.setDeliveryAddress(deliveryAddress);
        ctx.setDeliveryLat(lat);
        ctx.setDeliveryLng(lng);

        Order order = orderService.createOrder(ctx, items, grandTotal, ai);
        order.setPaymentMethod(paymentMethod);
        if (appliedPromoCode != null) {
            order.setPromoCode(appliedPromoCode);
            order.setDiscountAmount(discount);
        }

        // Increment promo usage counter
        if (promoCode != null && appliedPromoCode != null) {
            com.whatsappai.entity.Customer cust2 = customerService.findOrCreate(ctx.getBiz(), ctx.getPhone());
            PromotionService.DiscountResult dr = promotionService.validateAndApply(
                ctx.getBiz(), ctx.getPhone(), promoCode, total, cart, cust2);
            if (dr.promo() != null) promotionService.incrementUsage(dr.promo().getId());
        }

        // Persist the address to the customer record for future use
        customerService.updateAddress(ctx.getBiz(), ctx.getPhone(), deliveryAddress, lat, lng);

        // [CRITICAL] Decrement stock for each item
        for (OrderItem item : items) {
            int updated = productRepository.decrementStock(item.getProductId(), item.getQuantity());
            if (updated == 0) log.warn("Stock decrement may have failed for product {}", item.getProductId());
        }

        // Increment customer order count
        if (ctx.getCustomerId() != null) customerService.incrementOrderCount(ctx.getCustomerId());

        cartService.clearCart(ctx.getBiz(), ctx.getPhone());
        notificationService.notifyOwnerNewOrder(order, ctx.getBiz());

        StringBuilder reply = new StringBuilder("✅ *Order Confirmed!*\n");
        reply.append("Order ID: ORDER-").append(order.getId()).append("\n");
        if (discount.compareTo(BigDecimal.ZERO) > 0) {
            reply.append("Subtotal: ₹").append(total).append("\n");
            reply.append("🎉 Promo *").append(appliedPromoCode).append("* applied! You saved ₹").append(discount).append("\n");
            reply.append("Delivery: ₹").append(delivery).append("\n");
        }
        reply.append("*Total: ₹").append(grandTotal).append("*\n");
        if ("UPI".equals(paymentMethod) && ai.getUpiId() != null) {
            reply.append("\nPlease pay ₹").append(grandTotal)
                 .append(" to UPI: *").append(ai.getUpiId()).append("*\n");
            reply.append("Reference: ORDER-").append(order.getId()).append("\n");
            reply.append("Once paid, please let us know and our team will verify. 🙏");
        } else {
            reply.append("Payment: Cash on Delivery");
        }
        return FunctionResult.success(reply.toString(), order);
    }

    private FunctionResult cancelOrder(Map<String, Object> args, ExecutionContext ctx) {
        String reason = args.getOrDefault("reason", "Customer request").toString();
        UUID orderId = null;
        if (args.containsKey("order_id") && args.get("order_id") != null) {
            try { orderId = UUID.fromString(args.get("order_id").toString()); } catch (Exception ignored) {}
        }
        if (orderId == null) {
            // Use latest order
            return orderService.findLatestByPhone(ctx.getBiz(), ctx.getPhone())
                .map(o -> {
                    boolean ok = orderService.cancelOrder(o.getId(), ctx.getBiz(), ctx.getPhone(), reason);
                    return ok ? FunctionResult.success("Order cancelled. We're sorry to see you go!")
                              : FunctionResult.error("Order cannot be cancelled (status: " + o.getStatus() + ")");
                }).orElse(FunctionResult.error("No order found to cancel."));
        }
        boolean ok = orderService.cancelOrder(orderId, ctx.getBiz(), ctx.getPhone(), reason);
        return ok ? FunctionResult.success("Order cancelled successfully.")
                  : FunctionResult.error("Unable to cancel order. It may already be out for delivery.");
    }

    private FunctionResult checkOrderStatus(Map<String, Object> args, ExecutionContext ctx) {
        // 1. Try direct order ID lookup
        if (args.containsKey("order_id") && args.get("order_id") != null) {
            try {
                UUID orderId = UUID.fromString(args.get("order_id").toString());
                Order order = orderService.findByIdAndBusiness(orderId, ctx.getBiz()).orElse(null);
                if (order != null) {
                    return FunctionResult.success(formatOrderStatus(order));
                }
            } catch (Exception e) { /* fall through */ }
        }

        // 2. Get all customer orders
        List<Order> allOrders = orderService.findByCustomerPhone(ctx.getBiz(), ctx.getPhone());
        if (allOrders.isEmpty()) {
            return FunctionResult.error("You don't have any orders yet. Browse our products and place your first order! 🛍️");
        }

        // 3. Try product name search if provided
        String productName = args != null ? (String) args.get("product_name") : null;
        if (productName != null && !productName.isBlank()) {
            String searchLower = productName.toLowerCase();
            for (Order o : allOrders) {
                List<OrderItem> items = orderService.getItems(o.getId());
                for (OrderItem item : items) {
                    if (item.getProductName() != null && item.getProductName().toLowerCase().contains(searchLower)) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("Found your order containing *").append(item.getProductName()).append("*:\n\n");
                        sb.append(formatOrderStatus(o));
                        sb.append("\n📦 Items in this order:\n");
                        for (OrderItem oi : items) {
                            sb.append("  • ").append(oi.getProductName()).append(" x").append(oi.getQuantity())
                              .append(" — ₹").append(oi.getSubtotal()).append("\n");
                        }
                        return FunctionResult.success(sb.toString());
                    }
                }
            }
            return FunctionResult.error("No order found containing '" + productName + "'. Here are your recent orders:\n" + formatRecentOrders(allOrders));
        }

        // 4. No specific search — show latest order with items
        Order latest = allOrders.get(0);
        List<OrderItem> latestItems = orderService.getItems(latest.getId());
        StringBuilder sb = new StringBuilder("Here's your most recent order:\n\n");
        sb.append(formatOrderStatus(latest));
        if (!latestItems.isEmpty()) {
            sb.append("\n📦 Items:\n");
            for (OrderItem oi : latestItems) {
                sb.append("  • ").append(oi.getProductName()).append(" x").append(oi.getQuantity())
                  .append(" — ₹").append(oi.getSubtotal()).append("\n");
            }
        }
        if (allOrders.size() > 1) {
            sb.append("\nYou also have ").append(allOrders.size() - 1).append(" other order(s). Ask about a specific product to find it!");
        }
        return FunctionResult.success(sb.toString());
    }

    /** Format a single order's status nicely */
    private String formatOrderStatus(Order order) {
        StringBuilder sb = new StringBuilder();
        sb.append("🆔 Order: ORDER-").append(order.getId().toString().substring(0, 8)).append("\n");
        sb.append("📊 Status: *").append(order.getStatus()).append("*\n");
        sb.append("💰 Total: ₹").append(order.getTotalAmount()).append("\n");
        sb.append("📅 Placed: ").append(order.getCreatedAt().toLocalDate()).append("\n");
        if (order.getDeliveredAt() != null) {
            sb.append("✅ Delivered: ").append(order.getDeliveredAt().toLocalDate()).append("\n");
        }
        if (order.getDeliveryAddressText() != null) {
            sb.append("📍 Address: ").append(order.getDeliveryAddressText()).append("\n");
        }
        return sb.toString();
    }

    /** Format a list of recent orders as a summary */
    private String formatRecentOrders(List<Order> orders) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(orders.size(), 5); i++) {
            Order o = orders.get(i);
            sb.append("• ORDER-").append(o.getId().toString().substring(0, 8))
              .append(": *").append(o.getStatus()).append("* — ₹").append(o.getTotalAmount())
              .append(" (").append(o.getCreatedAt().toLocalDate()).append(")\n");
        }
        return sb.toString();
    }

    private FunctionResult repeatLastOrder(ExecutionContext ctx) {
        List<Order> delivered = orderService.findLastDelivered(ctx.getBiz(), ctx.getPhone());
        if (delivered.isEmpty()) return FunctionResult.error("No previous delivered order found.");
        Order last = delivered.get(0);
        List<OrderItem> items = orderService.getItems(last.getId());
        for (OrderItem item : items) {
            if (item.getProductId() == null) continue;
            productRepository.findByIdAndBusinessId(item.getProductId(), ctx.getBiz()).ifPresent(p -> {
                CartItem ci = CartItem.builder()
                    .productId(p.getId()).productName(p.getName())
                    .quantity(item.getQuantity()).unitPriceSnapshot(p.getPrice())
                    .category(p.getCategory()).build();
                cartService.addItem(ctx.getBiz(), ctx.getPhone(), ci);
            });
        }
        return FunctionResult.success("Added " + items.size() + " item(s) from your last order to cart! 🛒 Type 'confirm' to place order.");
    }

    private List<Product> findAlternatives(UUID biz, String category, UUID excludeId) {
        if (category == null) return List.of();
        return productRepository.findAlternativesByCategory(biz, category, excludeId,
            org.springframework.data.domain.PageRequest.of(0, 2));
    }

    /** NEW: Check status of customer's support tickets */
    private FunctionResult checkTicketStatus(ExecutionContext ctx) {
        var tickets = supportTicketService.findByPhone(ctx.getBiz(), ctx.getPhone());
        if (tickets.isEmpty()) {
            return FunctionResult.success("You don't have any support tickets. If you need to return a product or claim warranty, just let me know! 🙏");
        }
        StringBuilder sb = new StringBuilder("🎫 *Your Support Tickets:*\n\n");
        for (int i = 0; i < Math.min(tickets.size(), 5); i++) {
            var t = tickets.get(i);
            String icon = "RETURN".equals(t.getType()) ? "🔄" : "🛡️";
            sb.append(icon).append(" Ticket #").append(t.getId().toString().substring(0, 8)).append("\n");
            sb.append("   Type: ").append(t.getType()).append("\n");
            sb.append("   Product: ").append(t.getProductName()).append("\n");
            sb.append("   Status: *").append(t.getStatus()).append("*\n");
            if (t.getPolicyApplied() != null) sb.append("   Policy: ").append(t.getPolicyApplied()).append("\n");
            if (t.getAdminNotes() != null) sb.append("   Notes: ").append(t.getAdminNotes()).append("\n");
            sb.append("\n");
        }
        return FunctionResult.success(sb.toString());
    }

    /** NEW: List all active promotions */
    private FunctionResult listPromotions(ExecutionContext ctx) {
        var offers = promotionService.getActiveOffers(ctx.getBiz());
        if (offers.isEmpty()) {
            return FunctionResult.success("We don't have any active promotions right now, but check back soon! 🎁");
        }
        StringBuilder sb = new StringBuilder("🎉 *Current Deals & Offers:*\n\n");
        for (var o : offers) {
            String label = o.getOfferLabel() != null ? o.getOfferLabel() : (o.getDescription() != null ? o.getDescription() : o.getCode());
            sb.append("🏷️ *").append(label).append("*\n");
            if ("PERCENTAGE".equals(o.getDiscountType())) {
                sb.append("   ").append(o.getDiscountValue()).append("% off");
            } else {
                sb.append("   ₹").append(o.getDiscountValue()).append(" off");
            }
            if (o.getMinOrderAmount() != null) sb.append(" (min order ₹").append(o.getMinOrderAmount()).append(")");
            sb.append("\n");
            if (o.getCategory() != null) sb.append("   Applies to: ").append(o.getCategory()).append(" products\n");
            if (o.getBrand() != null) sb.append("   Brand: ").append(o.getBrand()).append("\n");
            if (Boolean.TRUE.equals(o.getFirstOrderOnly())) sb.append("   ⭐ First order only!\n");
            sb.append("   *Applied automatically at checkout* ✅\n\n");
        }
        return FunctionResult.success(sb.toString());
    }

    private FunctionResult raiseSupportTicket(Map<String, Object> args, ExecutionContext ctx) {
        try {
            String type = args.getOrDefault("type", "RETURN").toString().toUpperCase();
            String reason = args.getOrDefault("reason", "Customer request").toString();
            UUID productId = args.containsKey("product_id") ? UUID.fromString(args.get("product_id").toString()) : null;
            UUID orderId = args.containsKey("order_id") && args.get("order_id") != null
                ? UUID.fromString(args.get("order_id").toString()) : null;

            if (productId == null) {
                return FunctionResult.error("Please specify which product you'd like to " +
                    ("WARRANTY".equals(type) ? "claim warranty for." : "return."));
            }

            SupportTicketService.TicketResult result = supportTicketService.createTicket(
                ctx.getBiz(), ctx.getCustomerId(), ctx.getPhone(),
                productId, orderId, type, reason
            );

            if (!result.ok()) {
                return FunctionResult.error(result.message());
            }

            String ticketType = "RETURN".equals(type) ? "🔄 Return" : "🛡️ Warranty";
            StringBuilder reply = new StringBuilder("✅ *" + ticketType + " Ticket Created!*\n");
            reply.append("Ticket ID: ").append(result.ticket().getId().toString().split("-")[0]).append("\n");
            reply.append("Product: ").append(result.ticket().getProductName()).append("\n");
            if (result.ticket().getPolicyApplied() != null) {
                reply.append("Policy: ").append(result.ticket().getPolicyApplied()).append("\n");
            }
            reply.append("\nOur team will review your request and get back to you shortly. 🙏");
            return FunctionResult.success(reply.toString());
        } catch (Exception e) {
            log.error("Error creating support ticket", e);
            return FunctionResult.error("Could not create support ticket: " + e.getMessage());
        }
    }

    private FunctionResult requestVariantSelection(Map<String, Object> args, ExecutionContext ctx) {
        try {
            String productName = (String) args.get("product_name");
            List<Product> variants = productRepository.findByNameAndBusinessId(productName, ctx.getBiz());
            
            if (variants.isEmpty()) return FunctionResult.error("No variants found for " + productName);
            
            if (variants.size() <= 3) {
                // Send Buttons
                List<Map<String, String>> buttons = new ArrayList<>();
                for (Product v : variants) {
                    String title = (v.getSize() != null ? v.getSize() : "") + 
                                   (v.getColor() != null ? " " + v.getColor() : "");
                    if (title.isBlank()) title = "Option " + v.getSku();
                    buttons.add(Map.of("id", v.getId().toString(), "title", title));
                }
                whatsAppSender.sendButtons(ctx.getPhone(), "Please choose a variant for *" + productName + "*:", buttons, null);
            } else {
                // Send List
                List<Map<String, Object>> rows = new ArrayList<>();
                for (Product v : variants) {
                    String title = (v.getSize() != null ? v.getSize() : "") + 
                                   (v.getColor() != null ? " " + v.getColor() : "");
                    if (title.isBlank()) title = "Option " + v.getSku();
                    rows.add(Map.of("id", v.getId().toString(), "title", title, "description", "Price: ₹" + v.getPrice()));
                }
                whatsAppSender.sendList(ctx.getPhone(), "Select Variant", "Choose from the available options for *" + productName + "*:", 
                    List.of(Map.of("title", "Available Sizes/Colors", "rows", rows)), null);
            }
            
            return FunctionResult.success("I've sent the variant options to your phone! Please click one to select. 👆");
        } catch (Exception e) {
            return FunctionResult.error("Failed to show variants: " + e.getMessage());
        }
    }
}
