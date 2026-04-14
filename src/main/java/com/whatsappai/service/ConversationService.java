package com.whatsappai.service;

import com.whatsappai.ai.ModelGateway;
import com.whatsappai.ai.OllamaProvider;
import com.whatsappai.ai.dto.ModelRequest;
import com.whatsappai.ai.dto.ModelResponse;
import com.whatsappai.entity.*;
import com.whatsappai.model.*;
import com.whatsappai.qdrant.QdrantClient;
import com.whatsappai.repository.AISettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class ConversationService {

    private final TenantResolver tenantResolver;
    private final CustomerService customerService;
    private final SessionStore sessionStore;
    private final GuardrailService guardrailService;
    private final PromptBuilder promptBuilder;
    private final ModelGateway modelGateway;
    private final FunctionExecutor functionExecutor;
    private final SummarisationService summarisationService;
    private final WhatsAppSender whatsAppSender;
    private final NotificationService notificationService;
    private final ReverseGeocodeService geocodeService;
    private final ProductService productService;
    private final AISettingsRepository aiSettingsRepository;
    private final OllamaProvider ollamaProvider;
    private final QdrantClient qdrantClient;
    private final ChatPersistenceService chatPersistenceService;

    /** HTTP 200 is returned BEFORE this is called. @Async step 1b.
     *  NOTE: @Transactional is intentionally NOT on this method — @Async spawns a new thread
     *  so Spring's AOP proxy cannot bind a transaction to the calling thread context.
     *  Each DB save is wrapped individually via persistMessage() which carries @Transactional. */
    @Async("taskExecutor")
    public void processIncoming(String fromPhone, String toNumber, String messageType,
                                 String textBody, Double lat, Double lng, String rawMessageId) {
        String sessionId = UUID.randomUUID().toString();
        try {
            // Step 2: Tenant Resolve
            Optional<Business> bizOpt = tenantResolver.resolve(toNumber);
            if (bizOpt.isEmpty()) {
                log.warn("Unknown WA number: {} — ignoring", toNumber);
                return;
            }
            Business business = bizOpt.get();

            // Step 3: Phone Normalise + Block Check
            String phone = customerService.normalisePhone(fromPhone);
            Customer customer = customerService.findOrCreate(business.getId(), phone);

            // Step 3.5: Referral Extraction
            if (customer.getReferredBy() == null && textBody != null) {
                java.util.regex.Matcher refMatcher = java.util.regex.Pattern.compile("\\(Ref:\\s*(.*?)\\)").matcher(textBody);
                if (refMatcher.find()) {
                    String referrer = refMatcher.group(1).trim();
                    if (!referrer.isEmpty()) {
                        log.info("Adopting referral for customer {}: {}", phone, referrer);
                        customer.setReferredBy(referrer);
                        customerService.updateReferral(business.getId(), phone, referrer);
                        
                        // Push Real-Time Acquisition Event
                        notificationService.pushSseMarketingEvent(
                            business.getId(),
                            "UserPlus", 
                            "New Customer Acquired!", 
                            "Attributed to: " + referrer
                        );
                    }
                }
            }

            if (customer.isBlocked()) {
                log.info("Blocked customer {} — silent return", phone);
                return; // SILENT RETURN
            }

            if (Boolean.TRUE.equals(customer.getRequiresHuman())) {
                log.info("Customer {} requires human — mute AI", phone);
                // We still save the incoming message so the agent can see it in dashboard
                chatPersistenceService.persistMessage(business.getId(), phone, textBody, null, sessionId, false, null);
                return; // SILENT RETURN
            }

            // Step 4: Business Hours
            AISettings settings = aiSettingsRepository.findByBusinessId(business.getId()).orElseThrow();
            if (!isBusinessOpen(settings)) {
                String closedMsg = "Sorry, we're currently closed. Our hours are " +
                    settings.getOpenTime() + " to " + settings.getCloseTime() + ". Please message us then! 🙏";
                whatsAppSender.sendText(phone, closedMsg, business.getPhoneNumberId() != null ?
                    business.getPhoneNumberId() : settings.getBusinessId().toString());
                return;
            }

            // Step 5: Session Load
            ConversationSession session = sessionStore.getOrRebuild(business.getId(), phone);

            // Step 6: Name Gate
            if (customer.getName() == null && session.getStage() == ConvStage.GREETING) {
                if (!"AWAITING_NAME".equals(messageType)) {
                    String greet = "Hi! Welcome to *" + business.getName() + "*! 😊\nMay I know your name please?";
                    whatsAppSender.sendText(phone, greet, business.getPhoneNumberId());
                    session.setStage(ConvStage.AWAITING_NAME);
                    sessionStore.save(business.getId(), phone, session);
                    chatPersistenceService.persistMessage(business.getId(), phone, "[GREETING]", greet, sessionId, false, null);
                    return;
                }
            }
            if (session.getStage() == ConvStage.AWAITING_NAME && "text".equals(messageType)) {
                String name = extractName(textBody);
                customerService.updateName(business.getId(), phone, name);
                customer.setName(name);
                session.setCustomerName(name);
                session.setStage(ConvStage.BROWSING);
                String greeting = settings.getGreetingTemplate() != null
                    ? settings.getGreetingTemplate().replace("{name}", name)
                    : "Nice to meet you, *" + name + "*! 😊 How can I help you today? Type *show* to see our products.";
                whatsAppSender.sendText(phone, greeting, business.getPhoneNumberId());
                session.addMessage(SessionMessage.assistantMessage(greeting));
                sessionStore.save(business.getId(), phone, session);
                chatPersistenceService.persistMessage(business.getId(), phone, textBody, greeting, sessionId, false, null);
                return;
            }

            // Step 7: Type Routing
            String messageText;
            switch (messageType) {
                case "text" -> messageText = textBody != null ? textBody : "";
                case "image" -> {
                    String decline = "Sorry, I cannot process images at the moment 🙏\n" +
                        "Please describe what you're looking for in text and I'll help you!";
                    whatsAppSender.sendText(phone, decline, business.getPhoneNumberId());
                    chatPersistenceService.persistMessage(business.getId(), phone, "[IMAGE]", decline, sessionId, false, null);
                    return; // Stop pipeline
                }
                case "audio", "voice" -> {
                    String decline = "Sorry, I can't process voice messages 🙏\nPlease type your message!";
                    whatsAppSender.sendText(phone, decline, business.getPhoneNumberId());
                    chatPersistenceService.persistMessage(business.getId(), phone, "[VOICE]", decline, sessionId, false, null);
                    return;
                }
                case "location" -> {
                    String address = geocodeService.reverseGeocode(lat, lng);
                    session.setDeliveryAddress(address);
                    session.setDeliveryLat(lat);
                    session.setDeliveryLng(lng);
                    sessionStore.save(business.getId(), phone, session);
                    messageText = "My delivery address is: " + address;
                }
                case "sticker" -> messageText = "hi";
                default -> {
                    log.warn("Unhandled message type: {}", messageType);
                    return;
                }
            }

            // Step 7.5: Early persist IN
            // We use GENERAL domain for early persist if intent is not yet classified
            // or we'll update it later. Actually, intent WAS classified at step 8.
            
            // Step 8: Guardrail L1
            IntentResult intent = guardrailService.classify(messageText, settings);
            if (intent.isBlocked()) {
                chatPersistenceService.persistMessage(business.getId(), phone, messageText, intent.getSafeReply(),
                    sessionId, true, intent.getBlockReason());
                return;
            }

            // Trigger Bulk Lead Notification
            if (intent.getIntent() == com.whatsappai.model.IntentType.BULK) {
                notificationService.notifyOwnerBulkLead(business.getId(), phone, messageText);
            }

            // Step 9: Redis session already loaded (step 5)
            session.incrementMessageCount();

            // Step 10: Qdrant Memory (skip for greetings and first-time customers)
            List<String> memories = List.of();
            if (intent.getIntent() != IntentType.GREETING && customer.getTotalOrders() > 0) {
                try {
                    float[] emb = ollamaProvider.embed(messageText);
                    memories = qdrantClient.search("memories_" + business.getId(), emb, 3, phone);
                } catch (Exception e) {
                    log.warn("Qdrant search failed: {}", e.getMessage());
                }
            }

            // Step 11: Products
         // Step 11: Products — load for all intents except guardrail blocks
            List<com.whatsappai.entity.Product> products = List.of();
            if (intent.getIntent() != IntentType.POLITICS && intent.getIntent() != IntentType.MEDICAL
                    && intent.getIntent() != IntentType.LEGAL && intent.getIntent() != IntentType.ADULT
                    && intent.getIntent() != IntentType.RELIGION && intent.getIntent() != IntentType.COMPETITOR) {
                products = productService.searchRelevant(business.getId(), messageText, intent.getIntent());
            }

            // Step 11b: If ADDRESS intent, store in session
            if (intent.getIntent() == IntentType.ADDRESS && session.getDeliveryAddress() == null) {
                session.setDeliveryAddress(messageText);
                sessionStore.save(business.getId(), phone, session);
            }
            
            // Step 12: PromptBuilder
            ModelRequest modelRequest = promptBuilder.build(business, settings, customer, session, memories, products, messageText, intent.getIntent());

            // Step 13: LLM Call
            ModelResponse modelResponse = modelGateway.complete(modelRequest);

            // Step 13b: Tool Execution
            String assistantReply;
            if (modelResponse.isHasToolCall()) {
                ExecutionContext ctx = ExecutionContext.builder()
                    .biz(business.getId()).phone(phone).sessionId(sessionId)
                    .customerId(customer.getId())
                    .deliveryAddress(session.getDeliveryAddress())
                    .deliveryLat(session.getDeliveryLat())
                    .deliveryLng(session.getDeliveryLng())
                    .build();
                FunctionResult result = functionExecutor.execute(
                    modelResponse.getToolName(), modelResponse.getToolArguments(), ctx, session);
                assistantReply = result.getMessage();
                if (result.getData() instanceof com.whatsappai.entity.Order) {
                    session.setStage(ConvStage.ORDER_CONFIRMED);
                }
            } else {
                assistantReply = modelResponse.getContent();
                if (assistantReply == null || assistantReply.isBlank()) {
                    assistantReply = "I'm here to help! You can ask me to show products, check your order, view your cart, or find deals. What would you like? 😊";
                }
            }

            // Step 14: Sanitise (Layer 3)
            assistantReply = guardrailService.sanitise(assistantReply, products, settings);

            // Step 15: Persist OUT
            chatPersistenceService.persistMessage(business.getId(), phone, messageText, assistantReply, sessionId, false, null);

            // Step 16: Session Update
            session.addMessage(SessionMessage.userMessage(messageText));
            session.addMessage(SessionMessage.assistantMessage(assistantReply));
            sessionStore.save(business.getId(), phone, session);

            // Step 17: Async Memory (never blocks)
            if (session.getMessageCount() % 5 == 0 || session.getStage() == ConvStage.ORDER_CONFIRMED) {
                summarisationService.summariseAndStore(business.getId(), phone, session);
            }

            // Step 18: Send
            whatsAppSender.sendText(phone, assistantReply, business.getPhoneNumberId());
            notificationService.pushSseEvent(business.getId(), messageText, assistantReply, phone);

        } catch (Exception e) {
            log.error("Pipeline error for {}: {}", fromPhone, e.getMessage(), e);
        }
    }

    private boolean isBusinessOpen(AISettings settings) {
        if (settings.getOpenTime() == null || settings.getCloseTime() == null) return true;
        LocalTime now = LocalTime.now();
        return now.isAfter(settings.getOpenTime()) && now.isBefore(settings.getCloseTime());
    }

    private String extractName(String text) {
        if (text == null || text.isBlank()) return "Friend";
        String[] words = text.trim().split("\\s+");
        StringBuilder name = new StringBuilder();
        for (String w : words) {
            if (!w.equalsIgnoreCase("my") && !w.equalsIgnoreCase("name") &&
                !w.equalsIgnoreCase("is") && !w.equalsIgnoreCase("i") && !w.equalsIgnoreCase("am")) {
                if (!name.isEmpty()) name.append(" ");
                name.append(capitalize(w));
            }
        }
        return name.isEmpty() ? "Friend" : name.toString().trim();
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }

    // persistMessage() has been moved to ChatPersistenceService to fix the Spring
    // self-invocation proxy bypass issue: @Transactional on a method called via
    // "this.method()" is ignored because the AOP proxy is not involved.
}
