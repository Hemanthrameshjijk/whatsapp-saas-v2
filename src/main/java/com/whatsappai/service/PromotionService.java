package com.whatsappai.service;

import com.whatsappai.entity.Customer;
import com.whatsappai.entity.Promotion;
import com.whatsappai.model.CartItem;
import com.whatsappai.repository.PromotionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class PromotionService {

    private final PromotionRepository promotionRepository;

    /**
     * Validates ALL conditions and calculates the discount.
     * Returns a DiscountResult with either the discount amount or an error message.
     */
    public DiscountResult validateAndApply(UUID bizId, String phone, String promoCode,
                                            BigDecimal subtotal, Map<String, CartItem> cart,
                                            Customer customer) {
        if (promoCode == null || promoCode.isBlank()) {
            return DiscountResult.none();
        }

        String code = promoCode.trim().toUpperCase();
        Optional<Promotion> promoOpt = promotionRepository.findByBusinessIdAndCodeAndIsActiveTrue(bizId, code);

        if (promoOpt.isEmpty()) {
            return DiscountResult.error("Sorry, promo code *" + code + "* is not valid or has been deactivated.");
        }

        Promotion promo = promoOpt.get();
        LocalDateTime now = LocalDateTime.now();

        // 1. Check date range
        if (promo.getStartsAt() != null && now.isBefore(promo.getStartsAt())) {
            return DiscountResult.error("Promo code *" + code + "* is not active yet. Starts on " +
                promo.getStartsAt().toLocalDate() + ".");
        }
        if (promo.getExpiresAt() != null && now.isAfter(promo.getExpiresAt())) {
            return DiscountResult.error("Sorry, promo code *" + code + "* has expired.");
        }

        // 2. Check max uses
        if (promo.getMaxUses() != null && promo.getUsedCount() >= promo.getMaxUses()) {
            return DiscountResult.error("Sorry, promo code *" + code + "* has reached its usage limit.");
        }

        // 3. Check customer-specific targeting
        if (promo.getCustomerPhone() != null && !promo.getCustomerPhone().isBlank()) {
            if (!promo.getCustomerPhone().equals(phone)) {
                return DiscountResult.error("Sorry, promo code *" + code + "* is not available for your account.");
            }
        }

        // 4. Check first order only
        if (Boolean.TRUE.equals(promo.getFirstOrderOnly())) {
            if (customer.getTotalOrders() != null && customer.getTotalOrders() > 0) {
                return DiscountResult.error("Promo code *" + code + "* is only valid for your first order.");
            }
        }

        // 5. Check minimum order amount
        if (promo.getMinOrderAmount() != null && subtotal.compareTo(promo.getMinOrderAmount()) < 0) {
            BigDecimal diff = promo.getMinOrderAmount().subtract(subtotal);
            return DiscountResult.error("Add ₹" + diff.setScale(0, RoundingMode.UP) +
                " more to use promo *" + code + "* (min ₹" + promo.getMinOrderAmount().setScale(0) + ").");
        }

        // 6. Check product-specific
        if (promo.getProductId() != null) {
            boolean productInCart = cart.values().stream()
                .anyMatch(ci -> ci.getProductId().equals(promo.getProductId()));
            if (!productInCart) {
                return DiscountResult.error("Promo code *" + code + "* is only valid for a specific product that's not in your cart.");
            }
        }

        // 7. Check brand-specific
        if (promo.getBrand() != null && !promo.getBrand().isBlank()) {
            boolean brandInCart = cart.values().stream()
                .anyMatch(ci -> promo.getBrand().equalsIgnoreCase(ci.getBrand()));
            if (!brandInCart) {
                return DiscountResult.error("Promo code *" + code + "* is only valid for " + promo.getBrand() + " products.");
            }
        }

        // All checks passed — calculate discount
        BigDecimal discount;
        if ("PERCENTAGE".equals(promo.getDiscountType())) {
            discount = subtotal.multiply(promo.getDiscountValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            // Apply max discount cap
            if (promo.getMaxDiscount() != null && discount.compareTo(promo.getMaxDiscount()) > 0) {
                discount = promo.getMaxDiscount();
            }
        } else {
            // FLAT
            discount = promo.getDiscountValue();
        }

        // Ensure discount doesn't exceed subtotal
        if (discount.compareTo(subtotal) > 0) {
            discount = subtotal;
        }

        log.info("Promo {} applied: discount ₹{} (type={}, value={})", code, discount,
            promo.getDiscountType(), promo.getDiscountValue());

        return DiscountResult.success(discount, promo);
    }

    @Transactional
    public void incrementUsage(UUID promoId) {
        promotionRepository.incrementUsedCount(promoId);
    }

    /**
     * Find the best auto-apply offer for this customer's cart.
     * Picks the offer with the highest discount value.
     */
    public DiscountResult findBestOffer(UUID bizId, String phone, BigDecimal subtotal,
                                         Map<String, CartItem> cart, Customer customer) {
        List<Promotion> offers = promotionRepository.findActiveOffers(bizId);
        DiscountResult best = DiscountResult.none();

        for (Promotion offer : offers) {
            // Check customer targeting
            if (offer.getCustomerPhone() != null && !offer.getCustomerPhone().isBlank()
                && !offer.getCustomerPhone().equals(phone)) continue;
            // Check first order
            if (Boolean.TRUE.equals(offer.getFirstOrderOnly())
                && customer.getTotalOrders() != null && customer.getTotalOrders() > 0) continue;
            // Check min order
            if (offer.getMinOrderAmount() != null && subtotal.compareTo(offer.getMinOrderAmount()) < 0) continue;
            // Check product-specific
            if (offer.getProductId() != null) {
                boolean found = cart.values().stream().anyMatch(ci -> ci.getProductId().equals(offer.getProductId()));
                if (!found) continue;
            }
            // Check category
            if (offer.getCategory() != null && !offer.getCategory().isBlank()) {
                boolean found = cart.values().stream().anyMatch(ci ->
                    offer.getCategory().equalsIgnoreCase(ci.getCategory()));
                if (!found) continue;
            }

            // Check brand
            if (offer.getBrand() != null && !offer.getBrand().isBlank()) {
                boolean found = cart.values().stream().anyMatch(ci ->
                    offer.getBrand().equalsIgnoreCase(ci.getBrand()));
                if (!found) continue;
            }

            // Calculate discount
            BigDecimal discount;
            if ("PERCENTAGE".equals(offer.getDiscountType())) {
                discount = subtotal.multiply(offer.getDiscountValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                if (offer.getMaxDiscount() != null && discount.compareTo(offer.getMaxDiscount()) > 0)
                    discount = offer.getMaxDiscount();
            } else {
                discount = offer.getDiscountValue();
            }
            if (discount.compareTo(subtotal) > 0) discount = subtotal;

            if (discount.compareTo(best.discount()) > 0) {
                best = DiscountResult.success(discount, offer);
            }
        }
        return best;
    }

    /** Get all active offers (for AI prompt injection) */
    public List<Promotion> getActiveOffers(UUID bizId) {
        return promotionRepository.findActiveOffers(bizId);
    }

    // ── Dashboard CRUD ─────────────────────────────────────────

    public Page<Promotion> findAll(UUID bizId, Pageable pageable) {
        return promotionRepository.findByBusinessIdOrderByCreatedAtDesc(bizId, pageable);
    }

    @Transactional
    public Promotion create(UUID bizId, Promotion promo) {
        promo.setBusinessId(bizId);
        promo.setCode(promo.getCode().toUpperCase().trim());
        return promotionRepository.save(promo);
    }

    @Transactional
    public Promotion update(UUID id, UUID bizId, Promotion updates) {
        Promotion p = promotionRepository.findByIdAndBusinessId(id, bizId)
            .orElseThrow(() -> new RuntimeException("Promotion not found"));
        if (updates.getCode() != null) p.setCode(updates.getCode().toUpperCase().trim());
        if (updates.getDescription() != null) p.setDescription(updates.getDescription());
        if (updates.getDiscountType() != null) p.setDiscountType(updates.getDiscountType());
        if (updates.getDiscountValue() != null) p.setDiscountValue(updates.getDiscountValue());
        p.setMaxDiscount(updates.getMaxDiscount());
        p.setMinOrderAmount(updates.getMinOrderAmount());
        p.setProductId(updates.getProductId());
        p.setCustomerPhone(updates.getCustomerPhone());
        p.setFirstOrderOnly(updates.getFirstOrderOnly());
        p.setMaxUses(updates.getMaxUses());
        p.setIsActive(updates.getIsActive());
        p.setStartsAt(updates.getStartsAt());
        p.setExpiresAt(updates.getExpiresAt());
        p.setAutoApply(updates.getAutoApply());
        p.setOfferLabel(updates.getOfferLabel());
        p.setCategory(updates.getCategory());
        p.setBrand(updates.getBrand());
        return promotionRepository.save(p);
    }

    @Transactional
    public void delete(UUID id, UUID bizId) {
        promotionRepository.findByIdAndBusinessId(id, bizId).ifPresent(promotionRepository::delete);
    }

    // ── Result record ─────────────────────────────────────────

    public record DiscountResult(boolean applied, BigDecimal discount, String message, Promotion promo) {
        public static DiscountResult none() {
            return new DiscountResult(false, BigDecimal.ZERO, null, null);
        }
        public static DiscountResult success(BigDecimal discount, Promotion promo) {
            return new DiscountResult(true, discount, null, promo);
        }
        public static DiscountResult error(String message) {
            return new DiscountResult(false, BigDecimal.ZERO, message, null);
        }
    }
}
