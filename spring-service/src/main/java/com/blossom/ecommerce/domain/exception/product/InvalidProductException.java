package com.blossom.ecommerce.domain.exception.product;

import com.blossom.ecommerce.domain.exception.DomainException;

public class InvalidProductException extends DomainException {
    public InvalidProductException(String message) {
        super(message);
    }
}
