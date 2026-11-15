package com.blossom.ecommerce.domain.exception.account;

import com.blossom.ecommerce.domain.exception.DomainException;

public class InvalidCustomerException extends DomainException {
    public InvalidCustomerException(String message) {
        super(message);
    }
}
