package com.blossom.ecommerce.domain.exception.order;

import com.blossom.ecommerce.domain.exception.DomainException;

public class OrderNotFoundException extends DomainException {
    public OrderNotFoundException(Long id) {
        super("Order " + id + " not found");
    }
}
