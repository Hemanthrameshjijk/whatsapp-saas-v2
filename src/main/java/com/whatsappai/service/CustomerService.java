package com.whatsappai.service;

import com.whatsappai.entity.Customer;
import com.whatsappai.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;

    /** Normalize to E.164 format for Indian numbers. */
    public String normalisePhone(String phone) {
        if (phone == null) return phone;
        phone = phone.replaceAll("[^0-9+]", "");
        if (phone.startsWith("91") && phone.length() == 12) return "+" + phone;
        if (phone.startsWith("+91")) return phone;
        if (phone.length() == 10) return "+91" + phone;
        if (!phone.startsWith("+")) return "+" + phone;
        return phone;
    }

    @Transactional
    public Customer findOrCreate(UUID businessId, String phone) {
        return customerRepository.findByBusinessIdAndPhone(businessId, phone)
            .orElseGet(() -> customerRepository.save(Customer.builder()
                .businessId(businessId)
                .phone(phone)
                .build()));
    }

    @Transactional
    public void updateName(UUID businessId, String phone, String name) {
        customerRepository.updateName(businessId, phone, name);
    }

    @Transactional
    public void updateReferral(UUID businessId, String phone, String referrer) {
        Customer c = findOrCreate(businessId, phone);
        c.setReferredBy(referrer);
        customerRepository.save(c);
    }

    @Transactional
    public void setBlocked(UUID businessId, String phone, boolean blocked) {
        customerRepository.updateBlocked(businessId, phone, blocked);
    }

    public Optional<Customer> find(UUID businessId, String phone) {
        return customerRepository.findByBusinessIdAndPhone(businessId, phone);
    }

    public Page<Customer> findAll(UUID businessId, Pageable pageable) {
        return customerRepository.findByBusinessId(businessId, pageable);
    }

    @Transactional
    public void incrementOrderCount(UUID customerId) {
        customerRepository.incrementTotalOrders(customerId);
    }

    @Transactional
    public void setRequiresHuman(UUID businessId, String phone, boolean requiresHuman) {
        customerRepository.updateRequiresHuman(businessId, phone, requiresHuman);
    }

    @Transactional
    public void handoffToHuman(UUID businessId, String phone, String reason) {
        customerRepository.updateRequiresHumanWithReason(businessId, phone, true, reason);
    }

    @Transactional
    public void updateAddress(UUID businessId, String phone, String address, Double lat, Double lng) {
        Customer c = findOrCreate(businessId, phone);
        if (address != null) c.setLastDeliveryAddress(address);
        if (lat != null) c.setLastLat(lat);
        if (lng != null) c.setLastLng(lng);
        customerRepository.save(c);
    }
}
