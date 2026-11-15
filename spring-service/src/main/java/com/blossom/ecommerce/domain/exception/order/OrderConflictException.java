package com.blossom.ecommerce.domain.exception.order;

import com.blossom.ecommerce.domain.exception.DomainException;

/**
 * The order can't proceed given current state: not enough stock, or a status change that isn't allowed.
 */
public class OrderConflictException extends DomainException {
    public OrderConflictException(String message) {
        super(message);
    }
}
