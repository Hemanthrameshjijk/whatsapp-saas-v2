package com.whatsappai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FunctionResult {
    private boolean success;
    private String message;
    private Object data;

    public static FunctionResult success(String message) {
        return FunctionResult.builder().success(true).message(message).build();
    }

    public static FunctionResult success(String message, Object data) {
        return FunctionResult.builder().success(true).message(message).data(data).build();
    }

    public static FunctionResult error(String message) {
        return FunctionResult.builder().success(false).message(message).build();
    }
}
