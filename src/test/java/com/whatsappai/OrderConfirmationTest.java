package com.whatsappai;

import com.whatsappai.entity.*;
import com.whatsappai.model.*;
import com.whatsappai.repository.AISettingsRepository;
import com.whatsappai.repository.ProductRepository;
import com.whatsappai.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderConfirmationTest {

    @Mock ProductRepository productRepository;
    @Mock AISettingsRepository aiSettingsRepository;
    @Mock CartService cartService;
    @Mock OrderService orderService;
    @Mock NotificationService notificationService;
    @Mock CustomerService customerService;
    @InjectMocks FunctionExecutor functionExecutor;

    @Test
    void confirmOrder_alwaysUsesFreshPriceFromDB_notCartSnapshot() {
        UUID biz = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        String phone = "+919999999999";

        // Cart has snapshot price ₹1 (malicious/stale)
        CartItem cartItem = CartItem.builder()
            .productId(productId).productName("Blue Shirt")
            .quantity(2).unitPriceSnapshot(new BigDecimal("1.00")).build();
        Map<String, CartItem> cart = Map.of(productId.toString(), cartItem);
        when(cartService.getCart(biz, phone)).thenReturn(cart);

        // Database has real price ₹599
        Product dbProduct = Product.builder().id(productId).businessId(biz)
            .name("Blue Shirt").price(new BigDecimal("599.00")).stockQty(10)
            .category("Clothing").build();
        when(productRepository.findByIdAndBusinessId(productId, biz)).thenReturn(Optional.of(dbProduct));

        AISettings aiSettings = AISettings.builder().businessId(biz)
            .deliveryCharge(BigDecimal.ZERO).deliveryEnabled(false).build();
        when(aiSettingsRepository.findByBusinessId(biz)).thenReturn(Optional.of(aiSettings));

        Order mockOrder = Order.builder().id(UUID.randomUUID()).businessId(biz)
            .customerPhone(phone).totalAmount(new BigDecimal("1198.00")).build();
        when(orderService.createOrder(any(), any(), eq(new BigDecimal("1198.00")), any()))
            .thenReturn(mockOrder);
        when(productRepository.decrementStock(productId, 2)).thenReturn(1);
        doNothing().when(notificationService).notifyOwnerNewOrder(any(), any());

        ExecutionContext ctx = ExecutionContext.builder().biz(biz).phone(phone)
            .sessionId("s1").customerId(UUID.randomUUID()).build();
        ConversationSession session = new ConversationSession();

        FunctionResult result = functionExecutor.confirmOrder(Map.of("payment_method", "COD"), ctx, session);

        assertTrue(result.isSuccess());
        // Verify DB price (₹599 x 2 = ₹1198) was used, NOT cart snapshot (₹1 x 2 = ₹2)
        verify(productRepository).findByIdAndBusinessId(productId, biz);
        verify(orderService).createOrder(any(), any(), eq(new BigDecimal("1198.00")), any());
        verify(productRepository).decrementStock(productId, 2);
    }

    @Test
    void confirmOrder_emptyCart_returnsError() {
        UUID biz = UUID.randomUUID();
        String phone = "+919999999999";
        when(cartService.getCart(biz, phone)).thenReturn(Map.of());
        ExecutionContext ctx = ExecutionContext.builder().biz(biz).phone(phone).build();
        FunctionResult result = functionExecutor.confirmOrder(null, ctx, new ConversationSession());
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("empty"));
    }
}
