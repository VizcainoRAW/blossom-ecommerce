package com.blossom.ecommerce.domain.exception.account;

import com.blossom.ecommerce.domain.exception.DomainException;

public class InvalidCredentialsException extends DomainException {
    public InvalidCredentialsException() {
        super("Invalid identifier or password");
    }
}
