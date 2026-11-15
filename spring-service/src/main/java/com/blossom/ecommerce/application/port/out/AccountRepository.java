package com.blossom.ecommerce.application.port.out;

import com.blossom.ecommerce.domain.model.user.Account;

import java.util.Optional;

public interface AccountRepository {

    Optional<Account> findByIdentifier(String identifier);
}
