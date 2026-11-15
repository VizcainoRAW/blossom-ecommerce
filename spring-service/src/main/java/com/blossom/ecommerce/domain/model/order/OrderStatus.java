package com.blossom.ecommerce.domain.model.order;

import java.util.Set;

public enum OrderStatus {
    PENDING,
    PAID,
    SHIPPED,
    CANCELLED;

    public boolean canTransitionTo(OrderStatus next) {
        return switch (this) {
            case PENDING -> Set.of(PAID, CANCELLED).contains(next);
            case PAID -> Set.of(SHIPPED, CANCELLED).contains(next);
            case SHIPPED, CANCELLED -> false;
        };
    }
}
