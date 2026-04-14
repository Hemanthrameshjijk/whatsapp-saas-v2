package com.whatsappai.service;

import com.whatsappai.entity.Product;
import com.whatsappai.model.IntentType;
import com.whatsappai.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    public List<Product> searchRelevant(UUID businessId, String query, IntentType intent) {
        // For BROWSE or CHECKOUT always return ALL active products
        if (intent == IntentType.BROWSE || intent == IntentType.CHECKOUT
                || intent == IntentType.GREETING || query == null || query.isBlank()) {
            return productRepository.findByBusinessIdAndIsActiveTrue(businessId);
        }
        // For ADD_CART or GENERAL try name search first, fall back to all products
        List<Product> results = productRepository.findActiveByNameContaining(
                businessId, query, PageRequest.of(0, 10));
        if (results.isEmpty()) {
            return productRepository.findByBusinessIdAndIsActiveTrue(businessId);
        }
        return results;
    }

    public Optional<Product> findByIdAndBusiness(UUID productId, UUID businessId) {
        return productRepository.findByIdAndBusinessId(productId, businessId);
    }

    public List<Product> findAlternatives(UUID businessId, String category, UUID excludeId) {
        return productRepository.findAlternativesByCategory(businessId, category, excludeId,
                PageRequest.of(0, 2));
    }

    public Page<Product> findAll(UUID businessId, Pageable pageable) {
        return productRepository.findByBusinessId(businessId, pageable);
    }

    @Transactional
    public Product save(Product product) {
        return productRepository.save(product);
    }

    @Transactional
    public void softDelete(UUID id, UUID businessId) {
        productRepository.softDelete(id, businessId);
    }

    @Transactional
    public int decrementStock(UUID productId, int qty) {
        return productRepository.decrementStock(productId, qty);
    }
}