package com.whatsappai.exception;

import java.util.UUID;

public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(UUID id) { super("Order not found: " + id); }
    public OrderNotFoundException(String msg) { super(msg); }
}
