package com.blossom.ecommerce.domain.exception.account;

import com.blossom.ecommerce.domain.exception.DomainException;

public class IdentifierAlreadyRegisteredException extends DomainException {

    private final String identifier;

    public IdentifierAlreadyRegisteredException(String identifier) {
        super("the identifier " + identifier + " is already registered");
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }
}
