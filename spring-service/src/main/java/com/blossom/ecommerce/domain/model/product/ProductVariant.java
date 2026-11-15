package com.blossom.ecommerce.domain.model.product;

import com.blossom.ecommerce.domain.exception.product.InvalidProductException;

import java.math.BigDecimal;

/**
 * A sellable color/size combination of a product. {@code id} is null until persisted.
 */
public record ProductVariant(Long id, String color, String size, String sku, BigDecimal price, int stock) {

    public ProductVariant {
        if (color == null || color.isBlank()) throw new InvalidProductException("Variant color required");
        if (size == null || size.isBlank())   throw new InvalidProductException("Variant size required");
        if (sku == null || sku.isBlank())     throw new InvalidProductException("Variant SKU required");
        if (price == null || price.signum() < 0) throw new InvalidProductException("Variant price must be >= 0");
        if (stock < 0)                        throw new InvalidProductException("Variant stock must be >= 0");
        color = color.trim();
        size = size.trim();
        sku = sku.trim();
    }
}
