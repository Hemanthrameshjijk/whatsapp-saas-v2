package com.whatsappai.service;

import com.whatsappai.entity.Business;
import com.whatsappai.entity.Order;
import com.whatsappai.repository.BusinessRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {

    private final WhatsAppSender whatsAppSender;
    private final BusinessRepository businessRepository;

    // SSE emitters per business — timeout = Long.MAX_VALUE per spec
    private final Map<UUID, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter registerEmitter(UUID businessId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.computeIfAbsent(businessId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> removeEmitter(businessId, emitter));
        emitter.onTimeout(() -> removeEmitter(businessId, emitter));
        emitter.onError(e -> removeEmitter(businessId, emitter));
        return emitter;
    }

    public void pushSseEvent(UUID businessId, String customerMessage, String botReply, String customerPhone) {
        List<SseEmitter> list = emitters.get(businessId);
        if (list == null || list.isEmpty()) return;
        Map<String, String> data = Map.of(
            "type", "message",
            "phone", customerPhone,
            "customer", customerMessage,
            "bot", botReply
        );
        List<SseEmitter> dead = new ArrayList<>();
        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event().name("message").data(data));
            } catch (IOException e) {
                dead.add(emitter);
            }
        }
        list.removeAll(dead);
    }

    public void pushSseMarketingEvent(UUID businessId, String icon, String title, String description) {
        List<SseEmitter> list = emitters.get(businessId);
        if (list == null || list.isEmpty()) return;
        Map<String, String> data = Map.of(
            "type", "marketing",
            "icon", icon,
            "title", title,
            "description", description,
            "timestamp", String.valueOf(System.currentTimeMillis())
        );
        List<SseEmitter> dead = new ArrayList<>();
        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event().name("marketing").data(data));
            } catch (IOException e) {
                dead.add(emitter);
            }
        }
        list.removeAll(dead);
    }

    public void pushSseOrderEvent(UUID businessId, Order order) {
        List<SseEmitter> list = emitters.get(businessId);
        if (list == null || list.isEmpty()) return;
        
        Map<String, Object> data = Map.of(
            "type", "order",
            "id", order.getId().toString(),
            "phone", order.getCustomerPhone(),
            "total", order.getTotalAmount(),
            "status", order.getStatus(),
            "timestamp", String.valueOf(System.currentTimeMillis())
        );
        
        List<SseEmitter> dead = new ArrayList<>();
        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event().name("order").data(data));
            } catch (IOException e) {
                dead.add(emitter);
            }
        }
        list.removeAll(dead);
    }

    public void notifyOwnerNewOrder(Order order, UUID businessId) {
        try {
            businessRepository.findById(businessId).ifPresent(biz -> {
                String msg = "🛎️ *New Order!*\n" +
                    "ORDER-" + order.getId() + "\n" +
                    "Phone: " + order.getCustomerPhone() + "\n" +
                    "Total: ₹" + order.getTotalAmount();
                whatsAppSender.sendText(biz.getWhatsappNumber(), msg,
                    biz.getPhoneNumberId() != null ? biz.getPhoneNumberId() : biz.getWhatsappNumber());
                pushSseOrderEvent(businessId, order);
            });
        } catch (Exception e) {
            log.warn("Owner notification failed: {}", e.getMessage());
        }
    }

    /** 
     * Proactive alert for Wholesale/Bulk inquiries.
     * Logic: Alert the business owner about a high-value lead.
     */
    public void notifyOwnerBulkLead(UUID businessId, String customerPhone, String rawMessage) {
        try {
            businessRepository.findById(businessId).ifPresent(biz -> {
                String alert = "🚀 *NEW BULK LEAD!*\n" +
                    "Customer: " + customerPhone + "\n" +
                    "Message: \"" + rawMessage + "\"\n\n" +
                    "This inquiry was flagged as a high-volume B2B lead. Contact them ASAP! 📞";
                
                whatsAppSender.sendText(biz.getWhatsappNumber(), alert, biz.getPhoneNumberId());
                log.info("Sent Bulk Lead notification for biz={} customer={}", businessId, customerPhone);
            });
        } catch (Exception e) {
            log.warn("Bulk lead notification failed: {}", e.getMessage());
        }
    }

    private void removeEmitter(UUID businessId, SseEmitter emitter) {
        List<SseEmitter> list = emitters.get(businessId);
        if (list != null) list.remove(emitter);
    }
}
