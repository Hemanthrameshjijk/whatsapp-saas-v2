package com.whatsappai.exception;

import java.util.UUID;

public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException(UUID id) {
        super("Product not found: " + id);
    }
    public ProductNotFoundException(String msg) {
        super(msg);
    }
}
