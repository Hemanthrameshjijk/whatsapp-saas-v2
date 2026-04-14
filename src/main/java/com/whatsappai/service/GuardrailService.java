package com.whatsappai.service;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.whatsappai.entity.AISettings;
import com.whatsappai.entity.Product;
import com.whatsappai.model.IntentResult;
import com.whatsappai.model.IntentType;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class GuardrailService {

    private static final Map<IntentType, List<String>> KEYWORD_MAP = new LinkedHashMap<>();
    private static final List<String> PROFANITY_LIST = List.of("idiot","stupid","fool","bastard");

    static {
        KEYWORD_MAP.put(IntentType.POLITICS,
            List.of("vote","election","party","modi","government","minister","bjp","congress","protest","rally"));
        KEYWORD_MAP.put(IntentType.MEDICAL,
            List.of("medicine","fever","doctor","disease","tablet","hospital","injection","symptoms","cure","treatment"));
        KEYWORD_MAP.put(IntentType.LEGAL,
            List.of("court","lawyer","case","fir","police","complaint","sue","legal","arrest","bail","judgment"));
        KEYWORD_MAP.put(IntentType.ADULT,
            List.of("sex","porn","nude","xxx","adult","18+"));
        KEYWORD_MAP.put(IntentType.RELIGION,
            List.of("allah","jesus","temple","mosque","church","prayer","jihad","crusade"));
    }

    private static final Map<IntentType, List<String>> INTENT_MAP = new LinkedHashMap<>();
    static {
        INTENT_MAP.put(IntentType.GREETING,   List.of("hi","hello","hey","namaste","hola","vanakkam"));
        INTENT_MAP.put(IntentType.BROWSE,     List.of("show","products","items","list","catalogue","see","what do you have","what you have","available","menu","do you have","any ","have rice","have oil","have dal","have sugar","have atta","what all","tell me about","price of","cost of"));        INTENT_MAP.put(IntentType.ADD_CART,   List.of("add","want","order","buy","i need","give me","send me"));
        INTENT_MAP.put(IntentType.CHECKOUT,   List.of("confirm","place order","done","finalize","finalise","checkout","book"));
        INTENT_MAP.put(IntentType.ORDER_STATUS,List.of("status","where is my order","track","delivery update","when will"));
        INTENT_MAP.put(IntentType.CANCEL,     List.of("cancel","don't want","dont want","cancel my order","cancel order"));
        INTENT_MAP.put(IntentType.ADDRESS,    List.of("delivering to","my address","flat","door number","pin code","apartment","house no"));
        INTENT_MAP.put(IntentType.PAYMENT,    List.of("paid","upi","done paying","payment done","transferred","gpay","phonepe","paytm"));
        INTENT_MAP.put(IntentType.COMPLAINT,  List.of("not happy","problem","wrong item","broken","damaged","issue","complaint","bad quality"));
        INTENT_MAP.put(IntentType.PROMOTION,  List.of("offer","discount","deal","coupon","promo","code","sale","any offers","deals"));
        INTENT_MAP.put(IntentType.SUPPORT,    List.of("return","warranty","refund","replace","defective","ticket","claim","exchange"));
        INTENT_MAP.put(IntentType.BULK,       List.of("wholesale","bulk","quantity","b2b","business order","large order","container","palette","wholesale rate"));
        INTENT_MAP.put(IntentType.FRUSTRATED, List.of("useless","scam","worst","terrible","pathetic","waste","idiot","human","man","person","supervisor","manager","don't understand","dont understand"));
    }

    /** Layer 1: keyword block + intent classification. */
    public IntentResult classify(String message, AISettings settings) {
        if (message == null || message.isBlank()) return IntentResult.of(IntentType.GENERAL);
        String lower = message.toLowerCase();
        Set<String> allowlist = parseAllowlist(settings.getGuardrailAllowlist());

        // --- Check 6 guardrail categories ---
        for (Map.Entry<IntentType, List<String>> entry : KEYWORD_MAP.entrySet()) {
            for (String kw : entry.getValue()) {
                if (lower.contains(kw) && !allowlist.contains(kw)) {
                    log.info("Guardrail L1 blocked: category={} keyword={}", entry.getKey(), kw);
                    return IntentResult.blocked(entry.getKey(),
                        "I can only help with our store — what would you like to order? 😊");
                }
            }
        }

        // --- Competitor keywords (per-business) ---
        if (settings.getCompetitorKeywords() != null) {
            for (String kw : settings.getCompetitorKeywords().split(",")) {
                String kwl = kw.trim().toLowerCase();
                if (!kwl.isBlank() && lower.contains(kwl) && !allowlist.contains(kwl)) {
                    log.info("Guardrail L1 blocked: competitor keyword={}", kwl);
                    return IntentResult.blocked(IntentType.COMPETITOR,
                        "We have great options right here! What would you like to explore? 🛍️");
                }
            }
        }

        // --- Intent & Domain classification ---
        for (Map.Entry<IntentType, List<String>> entry : INTENT_MAP.entrySet()) {
            for (String kw : entry.getValue()) {
                if (lower.contains(kw)) {
                    return IntentResult.of(entry.getKey());
                }
            }
        }
        return IntentResult.of(IntentType.GENERAL);
    }

    /** Layer 3: post-LLM response sanitisation. */
    public String sanitise(String response, List<Product> products, AISettings settings) {
        if (response == null || response.isBlank()) return safeFallback();

        // Check 1 — Price hallucination
        response = checkPriceHallucination(response, products);
        if (response == null) {
            log.warn("Sanitiser: PRICE_HALLUCINATION blocked — returning safe fallback");
            return safeFallback();
        }

        // Check 2 — Competitor mention
        if (settings.getCompetitorKeywords() != null) {
            String lower = response.toLowerCase();
            for (String kw : settings.getCompetitorKeywords().split(",")) {
                if (!kw.isBlank() && lower.contains(kw.trim().toLowerCase())) {
                    log.warn("Sanitiser: COMPETITOR mention blocked");
                    return safeFallback();
                }
            }
        }

        // Check 3 — Off-topic re-scan (use word boundaries to avoid false positives
        // on product names that contain guardrail words, e.g. 'temple' brand shoes)
        for (List<String> kwList : KEYWORD_MAP.values()) {
            String lower = response.toLowerCase();
            for (String kw : kwList) {
                // Only block if the word appears as a standalone concept, not inside a product name
                // Check if the response is actually discussing the blocked topic
                String pattern = "\\b" + Pattern.quote(kw) + "\\b";
                if (Pattern.compile(pattern).matcher(lower).find()) {
                    // Double-check: don't block if this keyword appears in a product listing context
                    boolean isProductContext = lower.contains("₹") || lower.contains("cart") || lower.contains("stock");
                    if (!isProductContext) {
                        log.warn("Sanitiser: OFF_TOPIC re-scan blocked kw={}", kw);
                        return safeFallback();
                    }
                }
            }
        }

        // Check 4 — Profanity
        String lower = response.toLowerCase();
        for (String p : PROFANITY_LIST) {
            if (lower.contains(p)) {
                log.warn("Sanitiser: PROFANITY blocked");
                return safeFallback();
            }
        }
        return response;
    }

    private String checkPriceHallucination(String response, List<Product> products) {
        if (products == null || products.isEmpty()) return response;
        Pattern pricePattern = Pattern.compile("(?:₹|Rs\\.?)\\s*(\\d+(?:[.,]\\d+)?)");
        Matcher m = pricePattern.matcher(response);
        Set<BigDecimal> validPrices = new HashSet<>();
        for (Product p : products) if (p.getPrice() != null) validPrices.add(p.getPrice());

        while (m.find()) {
            try {
                String num = m.group(1).replace(",", "");
                BigDecimal mentioned = new BigDecimal(num);
                boolean valid = validPrices.stream().anyMatch(vp ->
                    vp.compareTo(mentioned) == 0 ||
                    vp.subtract(mentioned).abs().compareTo(BigDecimal.ONE) <= 0);
                if (!valid && !validPrices.isEmpty()) {
                    log.warn("Price hallucination: ₹{} not in valid prices {}", num, validPrices);
                    return null; // signal block
                }
            } catch (Exception ignored) {}
        }
        return response;
    }

    private Set<String> parseAllowlist(String raw) {
        if (raw == null || raw.isBlank()) return Set.of();
        Set<String> set = new HashSet<>();
        for (String s : raw.split(",")) if (!s.isBlank()) set.add(s.trim().toLowerCase());
        return set;
    }

    private static final List<String> SAFE_FALLBACKS = List.of(
        "I'm here to help you with shopping! What can I find for you today? 😊",
        "I can help you browse products, place orders, or check order status! What would you like to do? 🛍️",
        "Let me help you with our store — would you like to see our products or check an order? 😊",
        "I'm your shopping assistant! Tell me what you're looking for and I'll help you find it 🎯",
        "Hey! I can help with products, orders, returns and more. What do you need? 🙏"
    );

    private String safeFallback() {
        return SAFE_FALLBACKS.get(new java.util.Random().nextInt(SAFE_FALLBACKS.size()));
    }
}
