package com.blossom.ecommerce.application.port.in;

import java.math.BigDecimal;

/**
 * All filters are optional. A product matches the price range if any of its variants does.
 */
public record ProductSearchCriteria(String name, Long categoryId, BigDecimal minPrice, BigDecimal maxPrice,
                                    int page, int size) {

    public static final int MAX_PAGE_SIZE = 100;

    public ProductSearchCriteria {
        name = name == null || name.isBlank() ? null : name.trim();
        page = Math.max(page, 0);
        size = Math.clamp(size, 1, MAX_PAGE_SIZE);
    }
}
