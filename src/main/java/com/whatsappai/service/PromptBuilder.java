package com.whatsappai.service;

import com.whatsappai.ai.dto.*;
import com.whatsappai.entity.AISettings;
import com.whatsappai.entity.Business;
import com.whatsappai.entity.Customer;
import com.whatsappai.entity.Product;
import com.whatsappai.entity.Promotion;
import com.whatsappai.model.CartItem;
import com.whatsappai.model.ConversationSession;
import com.whatsappai.model.SessionMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
public class PromptBuilder {

    private final PromotionService promotionService;

    public PromptBuilder(PromotionService promotionService) {
        this.promotionService = promotionService;
    }

    @Value("${ai.temperature:0.4}")
    private double temperature;

    @Value("${ai.max-tokens:512}")
    private int maxTokens;

    public ModelRequest build(Business business, AISettings settings, Customer customer,
                               ConversationSession session, List<String> memories,
                               List<Product> products, String currentMessage, 
                               com.whatsappai.model.IntentType intentType) {
        String systemPrompt = buildSystemPrompt(business, settings, customer, memories, products, session);
        List<Message> history = buildHistory(session, currentMessage);
        List<ToolDefinition> tools = buildTools(intentType);

        return ModelRequest.builder()
            .model(settings.getActiveModel())
            .systemPrompt(systemPrompt)
            .conversationHistory(history)
            .tools(tools)
            .temperature(temperature)
            .maxTokens(maxTokens)
            .build();
    }

    private String buildSystemPrompt(Business business, AISettings settings, Customer customer,
                                     List<String> memories, List<Product> products, 
                                     ConversationSession session) {
        StringBuilder sb = new StringBuilder();

        // Block 1: Identity & Role
        sb.append("You are the friendly WhatsApp shopping assistant for *").append(business.getName()).append("*.\n\n");

        // Block 2: Thinking & Specialist Logic
        sb.append("=== HOW TO THINK ===\n");
        sb.append("- Before replying, think step-by-step about the customer's intent.\n");
        sb.append("- Use the specialized tools available for your role.\n");
        sb.append("- Reply naturally — do NOT use robotic or template-sounding language.\n\n");

        // Block 3: Safety Rules
        sb.append("=== SAFETY RULES ===\n");
        sb.append("- NEVER answer political, religious, medical, or legal questions. Politely redirect to shopping.\n");
        sb.append("- NEVER invent prices — only use prices from the AVAILABLE PRODUCTS list below.\n");
        sb.append("- NEVER make up delivery times not given to you.\n");
        sb.append("- NEVER mention or recommend competitor shops.\n");
        sb.append("- If a customer is angry, frustrated, or asks for a human, call `request_human_agent` IMMEDIATELY.\n\n");

        // Block 4: Personality
        sb.append("Language: ").append(settings.getLanguageStyle()).append(" — match the customer's language.\n");
        sb.append("Tone: ").append(settings.getToneStyle()).append(". Be warm, patient, and helpful.\n\n");

        // Block 5: Customer context
        sb.append("=== CUSTOMER INFO ===\n");
        if (customer.getName() != null) sb.append("Name: ").append(customer.getName()).append("\n");
        sb.append("Total past orders: ").append(customer.getTotalOrders()).append("\n");
        if (customer.getLastDeliveryAddress() != null) {
            sb.append("SAVED ADDRESS: ").append(customer.getLastDeliveryAddress()).append("\n");
        }

        // Block 6: Qdrant memories
        if (!memories.isEmpty()) {
            sb.append("\n=== CUSTOMER MEMORY (past interactions) ===\n");
            for (String m : memories) sb.append("- ").append(m).append("\n");
        }

        // Block 7: Products
        if (!products.isEmpty()) {
            sb.append("\n=== AVAILABLE PRODUCTS (prices from database — do not invent) ===\n");
            for (Product p : products) {
                sb.append("• [").append(p.getId()).append("] ").append(p.getName());
                if (p.getBrand() != null && !p.getBrand().isBlank()) sb.append(" (").append(p.getBrand()).append(")");
                sb.append(" — ₹").append(p.getPrice());

                List<String> details = new ArrayList<>();
                if (p.getSku() != null && !p.getSku().isBlank()) details.add("SKU: " + p.getSku());
                if (p.getType() != null && !p.getType().isBlank()) details.add("Type: " + p.getType());
                if (p.getColor() != null && !p.getColor().isBlank()) details.add("Color: " + p.getColor());
                if (p.getSize() != null && !p.getSize().isBlank()) details.add("Size: " + p.getSize());
                if (!details.isEmpty()) sb.append(" | ").append(String.join(", ", details));

                if (p.getDescription() != null && !p.getDescription().isBlank()) sb.append(" | ").append(p.getDescription());

                if (Boolean.TRUE.equals(p.getOutOfStock())) {
                    sb.append(" ❌ OUT OF STOCK\n");
                } else {
                    sb.append(" (stock: ").append(p.getStockQty()).append(")\n");
                }

                if ("CUSTOM".equals(p.getWarrantyMode())) {
                    sb.append("   🛡️ Warranty: ").append(p.getWarrantyDetails()).append("\n");
                } else if ("ENABLED".equals(settings.getGlobalWarrantyMode())) {
                    sb.append("   🛡️ Warranty: ").append(settings.getGlobalWarrantyDetails()).append("\n");
                }

                if ("CUSTOM".equals(p.getReturnMode())) {
                    sb.append("   🔄 Returns: ").append(p.getCustomReturnPolicy()).append("\n");
                } else if ("ENABLED".equals(settings.getGlobalReturnMode())) {
                    sb.append("   🔄 Returns: ").append(settings.getGlobalReturnPolicy()).append("\n");
                }
            }
        }

        // Block 8: Cart
        if (!session.getCart().isEmpty()) {
            sb.append("\n=== CURRENT CART ===\n");
            for (CartItem ci : session.getCart().values()) {
                sb.append("• ").append(ci.getProductName()).append(" x").append(ci.getQuantity())
                  .append(" @ ₹").append(ci.getUnitPriceSnapshot()).append("\n");
            }
        }

        // Block 9: Business settings
        sb.append("\n=== STORE SETTINGS ===\n");
        if (Boolean.TRUE.equals(settings.getDeliveryEnabled())) {
            sb.append("Delivery charge: ₹").append(settings.getDeliveryCharge()).append("\n");
        }
        if (settings.getUpiId() != null) sb.append("UPI ID: ").append(settings.getUpiId()).append("\n");
        if (settings.getShopAddress() != null) sb.append("Shop address: ").append(settings.getShopAddress()).append("\n");
        if (settings.getGeneralFaq() != null) sb.append("Store FAQ: ").append(settings.getGeneralFaq()).append("\n");

        // Block 10: Active Promotions
        try {
            List<Promotion> offers = promotionService.getActiveOffers(business.getId());
            if (!offers.isEmpty()) {
                sb.append("\n=== ACTIVE PROMOTIONS & OFFERS ===\n");
                for (Promotion o : offers) {
                    sb.append("🏷️ ").append(o.getOfferLabel() != null ? o.getOfferLabel() : o.getCode());
                    sb.append(" — ");
                    if ("PERCENTAGE".equals(o.getDiscountType())) sb.append(o.getDiscountValue()).append("% off");
                    else sb.append("₹").append(o.getDiscountValue()).append(" off");
                    sb.append("\n");
                }
            }
        } catch (Exception ignored) {}

        // Block 11: Tool Usage Guidelines
        sb.append("\n=== TOOL USAGE ===\n");
        sb.append("- 'Add X' → `add_to_cart`.\n");
        sb.append("- 'Show cart' → `view_cart`.\n");
        sb.append("- 'Place order' → `confirm_order` (Ask for address if missing).\n");
        sb.append("- 'Where is my order?' → `check_order_status`.\n");
        sb.append("- 'Return/Warranty' → `raise_support_ticket`.\n");
        sb.append("- 'Help/Human' → `request_human_agent`.\n");

        return sb.toString();
    }


    private List<Message> buildHistory(ConversationSession session, String currentMessage) {
        List<Message> history = new ArrayList<>();
        for (SessionMessage sm : session.getLast(10)) {
            history.add(Message.builder().role(sm.getRole()).content(sm.getContent()).build());
        }
        history.add(Message.builder().role("user").content(currentMessage).build());
        return history;
    }

    private List<ToolDefinition> buildTools(com.whatsappai.model.IntentType intent) {
        List<ToolDefinition> tools = new ArrayList<>();
        if (intent == com.whatsappai.model.IntentType.GREETING) return tools;

        tools.add(tool("add_to_cart", "Add item to cart", Map.of("type","object","properties",Map.of("product_id",Map.of("type","string"),"quantity",Map.of("type","integer")))));
        tools.add(tool("view_cart", "Show cart content", Map.of("type","object","properties",Map.of())));
        tools.add(tool("confirm_order", "Finalize checkout", Map.of("type","object","properties",Map.of("payment_method",Map.of("type","string","enum",List.of("UPI","COD")),"delivery_address",Map.of("type","string")))));
        
        tools.add(tool("quick_buy", "Direct one-step purchase for exactly ONE product. Bypasses the cart. Use if a customer says 'I want to buy X' and they don't have other items in their cart.", 
            Map.of("type","object","properties",Map.of(
                "product_id",Map.of("type","string","description","UUID of the single product"),
                "quantity",Map.of("type","integer","description","Quantity (default 1)"),
                "delivery_address",Map.of("type","string","description","Collected address"),
                "payment_method",Map.of("type","string","enum",List.of("UPI","COD"))
            ),"required",List.of("product_id"))));

        tools.add(tool("check_order_status", "Track order", Map.of("type","object","properties",Map.of("order_id",Map.of("type","string")))));
        tools.add(tool("request_human_agent", "Talk to human", Map.of("type","object","properties",Map.of("reason",Map.of("type","string")))));
        tools.add(tool("raise_support_ticket", "Return/Warranty claim", Map.of("type","object","properties",Map.of("type",Map.of("type","string","enum",List.of("RETURN","WARRANTY")),"product_id",Map.of("type","string"),"reason",Map.of("type","string")))));
        tools.add(tool("list_promotions", "Show offers", Map.of("type","object","properties",Map.of())));
        
        tools.add(tool("request_variant_selection", "Ask the customer to select a variant (size, color, etc) using interactive buttons or lists.", 
            Map.of("type","object","properties",Map.of(
                "product_name",Map.of("type","string","description","The common name of the product (e.g., 'Nike Running Shoe')")
            ),"required",List.of("product_name"))));
        
        return tools;
    }

    private ToolDefinition tool(String name, String description, Map<String, Object> parameters) {
        return ToolDefinition.builder().type("function").function(ToolDefinition.FunctionDef.builder().name(name).description(description).parameters(parameters).build()).build();
    }
}
