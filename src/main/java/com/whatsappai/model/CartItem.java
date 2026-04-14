package com.whatsappai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItem {
    private UUID productId;
    private String productName;
    private int quantity;
    /** Snapshot price — stored in Redis but NEVER used for order totals.
     *  confirmOrder() always re-fetches price from PostgreSQL. */
    private BigDecimal unitPriceSnapshot;
    private String category;
    private String brand;
}
