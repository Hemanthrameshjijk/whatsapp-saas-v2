package com.whatsappai.service;

import com.whatsappai.entity.AISettings;
import com.whatsappai.model.CartItem;
import com.whatsappai.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class CartService {

    @Value("${redis.session-ttl-minutes:30}")
    private long ttl;

    private final RedisTemplate<String, Object> redisTemplate;

    private String cartKey(UUID biz, String phone) {
        return "cart:" + biz + ":" + phone;
    }

    @SuppressWarnings("unchecked")
    public Map<String, CartItem> getCart(UUID biz, String phone) {
        try {
            Object val = redisTemplate.opsForValue().get(cartKey(biz, phone));
            if (val instanceof Map<?,?> map) {
                // Deserialize from LinkedHashMap (Jackson default) to CartItem
                Map<String, CartItem> cart = new LinkedHashMap<>();
                for (Map.Entry<?,?> e : map.entrySet()) {
                    if (e.getValue() instanceof CartItem ci) {
                        cart.put(e.getKey().toString(), ci);
                    } else if (e.getValue() instanceof Map<?,?> inner) {
                        // Jackson may deserialize as LinkedHashMap; convert manually
                        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
                        CartItem ci2 = mapper.convertValue(inner, CartItem.class);
                        cart.put(e.getKey().toString(), ci2);
                    }
                }
                return cart;
            }
        } catch (Exception e) {
            log.warn("Failed to get cart from Redis for {}: {}", phone, e.getMessage());
        }
        return new LinkedHashMap<>();
    }

    public void addItem(UUID biz, String phone, CartItem item) {
        Map<String, CartItem> cart = getCart(biz, phone);
        String key = item.getProductId().toString();
        if (cart.containsKey(key)) {
            CartItem existing = cart.get(key);
            existing.setQuantity(existing.getQuantity() + item.getQuantity());
        } else {
            cart.put(key, item);
        }
        saveCart(biz, phone, cart);
    }

    public void removeItem(UUID biz, String phone, UUID productId) {
        Map<String, CartItem> cart = getCart(biz, phone);
        cart.remove(productId.toString());
        saveCart(biz, phone, cart);
    }

    public void clearCart(UUID biz, String phone) {
        try {
            redisTemplate.delete(cartKey(biz, phone));
        } catch (Exception e) {
            log.warn("Failed to clear cart in Redis for {}: {}", phone, e.getMessage());
        }
    }

    private void saveCart(UUID biz, String phone, Map<String, CartItem> cart) {
        try {
            redisTemplate.opsForValue().set(cartKey(biz, phone), cart, ttl, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("Failed to save cart to Redis for {}: {}", phone, e.getMessage());
        }
    }

    public BigDecimal calcDeliveryCharge(BigDecimal orderTotal, AISettings settings) {
        if (!Boolean.TRUE.equals(settings.getDeliveryEnabled())) return BigDecimal.ZERO;
        BigDecimal freeAbove = settings.getFreeDeliveryAbove();
        if (freeAbove != null && orderTotal.compareTo(freeAbove) >= 0) return BigDecimal.ZERO;
        return settings.getDeliveryCharge() != null ? settings.getDeliveryCharge() : BigDecimal.ZERO;
    }

    public String formatCart(Map<String, CartItem> cart, AISettings settings) {
        if (cart.isEmpty()) return "Your cart is empty.";
        StringBuilder sb = new StringBuilder("🛒 *Your Cart:*\n");
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem ci : cart.values()) {
            BigDecimal lineTotal = ci.getUnitPriceSnapshot().multiply(BigDecimal.valueOf(ci.getQuantity()));
            sb.append("• ").append(ci.getProductName())
              .append(" x").append(ci.getQuantity())
              .append(" = ₹").append(lineTotal).append("\n");
            subtotal = subtotal.add(lineTotal);
        }
        BigDecimal delivery = calcDeliveryCharge(subtotal, settings);
        sb.append("\nSubtotal: ₹").append(subtotal);
        sb.append("\nDelivery: ₹").append(delivery);
        sb.append("\n*Total: ₹").append(subtotal.add(delivery)).append("*");
        return sb.toString();
    }

    public Map<String, Map<String, CartItem>> getActiveCarts(UUID biz) {
        try {
            Set<String> keys = redisTemplate.keys("cart:" + biz + ":*");
            Map<String, Map<String, CartItem>> result = new HashMap<>();
            if (keys == null) return result;
            for (String key : keys) {
                String phone = key.substring(key.lastIndexOf(":") + 1);
                result.put(phone, getCart(biz, phone));
            }
            return result;
        } catch (Exception e) {
            log.warn("Failed to list active carts from Redis for business {}: {}", biz, e.getMessage());
            return new HashMap<>();
        }
    }
}
