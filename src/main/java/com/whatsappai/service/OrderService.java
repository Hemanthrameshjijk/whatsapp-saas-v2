package com.whatsappai.service;

import com.whatsappai.entity.Order;
import com.whatsappai.entity.OrderItem;
import com.whatsappai.model.ExecutionContext;
import com.whatsappai.repository.OrderItemRepository;
import com.whatsappai.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    @Transactional
    public Order createOrder(ExecutionContext ctx, List<OrderItem> items,
                             BigDecimal grandTotal, com.whatsappai.entity.AISettings settings) {
        Order order = Order.builder()
            .businessId(ctx.getBiz())
            .customerId(ctx.getCustomerId())
            .customerPhone(ctx.getPhone())
            .totalAmount(grandTotal)
            .status("CONFIRMED")
            .confirmedAt(LocalDateTime.now())
            .deliveryAddressText(ctx.getDeliveryAddress())
            .deliveryLat(ctx.getDeliveryLat())
            .deliveryLng(ctx.getDeliveryLng())
            .addressSource(ctx.getDeliveryAddress() != null ? "TEXT" : null)
            .build();
        Order saved = orderRepository.save(order);

        for (OrderItem item : items) {
            item = OrderItem.builder()
                .orderId(saved.getId())
                .productId(item.getProductId())
                .productName(item.getProductName())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .subtotal(item.getSubtotal())
                .build();
            orderItemRepository.save(item);
        }
        return saved;
    }

    public Optional<Order> findByIdAndBusiness(UUID orderId, UUID businessId) {
        return orderRepository.findByIdAndBusinessId(orderId, businessId);
    }

    public List<Order> findByCustomerPhone(UUID businessId, String phone) {
        return orderRepository.findByBusinessIdAndCustomerPhoneOrderByCreatedAtDesc(businessId, phone);
    }

    public Optional<Order> findLatestByPhone(UUID businessId, String phone) {
        return findByCustomerPhone(businessId, phone).stream().findFirst();
    }

    public List<Order> findLastDelivered(UUID businessId, String phone) {
        return orderRepository.findLastDelivered(businessId, phone, PageRequest.of(0, 1));
    }

    public Page<Order> findAll(UUID businessId, Pageable pageable) {
        return orderRepository.findByBusinessId(businessId, pageable);
    }

    public Page<Order> findByStatus(UUID businessId, String status, Pageable pageable) {
        return orderRepository.findByBusinessIdAndStatus(businessId, status, pageable);
    }

    @Transactional
    public void updateStatus(UUID orderId, UUID businessId, String status) {
        orderRepository.updateStatus(orderId, businessId, status);
        if ("DELIVERED".equals(status)) {
            orderRepository.findByIdAndBusinessId(orderId, businessId).ifPresent(o -> {
                o.setDeliveredAt(LocalDateTime.now());
                orderRepository.save(o);
            });
        }
    }

    @Transactional
    public void markPaid(UUID orderId, UUID businessId) {
        orderRepository.findByIdAndBusinessId(orderId, businessId).ifPresent(o -> {
            o.setPaymentStatus("PAID");
            o.setDeliveredAt(LocalDateTime.now());
            orderRepository.save(o);
        });
    }

    @Transactional
    public boolean cancelOrder(UUID orderId, UUID businessId, String phone, String reason) {
        Optional<Order> opt = orderRepository.findByIdAndBusinessId(orderId, businessId);
        if (opt.isEmpty()) return false;
        Order o = opt.get();
        if (!o.getCustomerPhone().equals(phone)) return false;
        if (!"PENDING".equals(o.getStatus()) && !"CONFIRMED".equals(o.getStatus())) return false;
        o.setStatus("CANCELLED");
        o.setCancellationReason(reason);
        orderRepository.save(o);
        return true;
    }

    @Transactional
    public void updateAddress(UUID orderId, UUID businessId, String address) {
        orderRepository.findByIdAndBusinessId(orderId, businessId).ifPresent(o -> {
            o.setDeliveryAddressText(address);
            o.setAddressSource("TEXT");
            orderRepository.save(o);
        });
    }

    public List<OrderItem> getItems(UUID orderId) {
        return orderItemRepository.findByOrderId(orderId);
    }
}
