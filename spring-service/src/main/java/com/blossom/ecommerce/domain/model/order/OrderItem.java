package com.blossom.ecommerce.domain.model.order;

import com.blossom.ecommerce.domain.exception.order.InvalidOrderException;

import java.math.BigDecimal;

/**
 * One line of an order. Name, SKU, color, size and price are copied from the variant when the order is placed.
 */
public record OrderItem(Long variantId, String productName, String sku, String color, String size,
                        int quantity, BigDecimal unitPrice) {

    public OrderItem {
        if (variantId == null)                         throw new InvalidOrderException("Variant required");
        if (quantity <= 0)                             throw new InvalidOrderException("Quantity must be > 0");
        if (unitPrice == null || unitPrice.signum() < 0) throw new InvalidOrderException("Unit price must be >= 0");
    }

    public BigDecimal subtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
