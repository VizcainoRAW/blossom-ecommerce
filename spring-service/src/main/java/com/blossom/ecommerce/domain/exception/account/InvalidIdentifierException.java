package com.blossom.ecommerce.domain.exception.account;

import com.blossom.ecommerce.domain.exception.DomainException;

public class InvalidIdentifierException extends DomainException {
    public InvalidIdentifierException(String message) {
        super(message);
    }
}
