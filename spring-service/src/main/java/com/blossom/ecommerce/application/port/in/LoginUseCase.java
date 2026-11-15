package com.blossom.ecommerce.application.port.in;

import com.blossom.ecommerce.application.port.out.AccountRepository;
import com.blossom.ecommerce.application.port.out.PasswordEncoderPort;
import com.blossom.ecommerce.domain.exception.account.InvalidCredentialsException;
import com.blossom.ecommerce.domain.model.user.Account;

public class LoginUseCase {

    private final AccountRepository accountRepository;
    private final PasswordEncoderPort encoder;

    public LoginUseCase(AccountRepository accountRepository, PasswordEncoderPort encoder) {
        this.accountRepository = accountRepository;
        this.encoder = encoder;
    }

    public Account login(String identifier, String password) {
        if (identifier == null || password == null) throw new InvalidCredentialsException();
        return accountRepository.findByIdentifier(identifier.trim().toLowerCase())
                .filter(a -> encoder.matches(password, a.getPasswordHash()))
                .filter(Account::isActive)
                .orElseThrow(InvalidCredentialsException::new);
    }
}
