package com.blossom.ecommerce.domain.exception.order;

import com.blossom.ecommerce.domain.exception.DomainException;

public class InvalidOrderException extends DomainException {
    public InvalidOrderException(String message) {
        super(message);
    }
}
