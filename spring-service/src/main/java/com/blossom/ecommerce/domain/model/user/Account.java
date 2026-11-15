package com.blossom.ecommerce.domain.model.user;

import com.blossom.ecommerce.domain.exception.account.InvalidIdentifierException;
import com.blossom.ecommerce.domain.model.user.valueobject.AccountType;

import java.util.UUID;

public abstract class Account {

    private final UUID  id;

    private final AccountType accountType;

    private final String identifier;

    private final String passwordHash;

    private boolean active;

    protected Account(UUID id, AccountType type, String identifier, String passwordHash, boolean active) {
        if (identifier == null || identifier.isBlank()) throw new InvalidIdentifierException("Identifier required");

        this.id = id;
        this.accountType = type;
        this.identifier = identifier.trim().toLowerCase();
        this.passwordHash = passwordHash;
        this.active = active;
    }

    public UUID getId() { return id; }
    public AccountType getAccountType() { return accountType; }
    public String getIdentifier() { return identifier; }
    public String getPasswordHash() { return passwordHash; }
    public boolean isActive() { return active; }


    @Override
    public String toString() {
        return "Account(%s/%s)".formatted(id, identifier);
    }
}
