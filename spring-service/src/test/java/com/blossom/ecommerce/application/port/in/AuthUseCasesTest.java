package com.blossom.ecommerce.application.port.in;

import com.blossom.ecommerce.application.port.out.AccountRepository;
import com.blossom.ecommerce.application.port.out.CustomerRepository;
import com.blossom.ecommerce.application.port.out.PasswordEncoderPort;
import com.blossom.ecommerce.domain.exception.account.IdentifierAlreadyRegisteredException;
import com.blossom.ecommerce.domain.exception.account.InvalidCredentialsException;
import com.blossom.ecommerce.domain.model.user.Account;
import com.blossom.ecommerce.domain.model.user.Customer;
import com.blossom.ecommerce.domain.model.user.valueobject.DocumentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthUseCasesTest {

    @Mock CustomerRepository customers;
    @Mock AccountRepository accounts;
    @Mock PasswordEncoderPort encoder;

    @Test
    void registerHashesPasswordAndSaves() {
        when(customers.existsByIdentifier("ana@mail.com")).thenReturn(false);
        when(encoder.encode("secret123")).thenReturn("HASH");
        when(customers.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Customer saved = new RegisterCustomerUseCase(customers, encoder)
                .registerCustomer("Ana", "Diaz", " Ana@Mail.com ", "secret123", DocumentType.CC, "1");

        assertThat(saved.getIdentifier()).isEqualTo("ana@mail.com");
        assertThat(saved.getPasswordHash()).isEqualTo("HASH");
    }

    @Test
    void registerRejectsIdentifierInAnyCase() {
        when(customers.existsByIdentifier("ana@mail.com")).thenReturn(true);

        assertThatThrownBy(() -> new RegisterCustomerUseCase(customers, encoder)
                .registerCustomer("Ana", "Diaz", "ANA@mail.com", "secret123", null, null))
                .isInstanceOf(IdentifierAlreadyRegisteredException.class)
                .satisfies(e -> assertThat(((IdentifierAlreadyRegisteredException) e).getIdentifier()).isEqualTo("ANA@mail.com"));
        verify(customers, never()).save(any());
    }

    private Customer customer(boolean active) {
        return Customer.restore(UUID.randomUUID(), "ana@mail.com", "HASH", active, "Ana", "Diaz", null, null);
    }

    @Test
    void loginReturnsAccountForValidCredentials() {
        Customer c = customer(true);
        when(accounts.findByIdentifier("ana@mail.com")).thenReturn(Optional.of(c));
        when(encoder.matches("secret123", "HASH")).thenReturn(true);

        Account result = new LoginUseCase(accounts, encoder).login(" ANA@mail.com ", "secret123");

        assertThat(result).isSameAs(c);
    }

    @Test
    void loginFailsTheSameWayForEveryBadCase() {
        LoginUseCase login = new LoginUseCase(accounts, encoder);

        when(accounts.findByIdentifier("nobody@mail.com")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> login.login("nobody@mail.com", "x")).isInstanceOf(InvalidCredentialsException.class);

        when(accounts.findByIdentifier("ana@mail.com")).thenReturn(Optional.of(customer(true)));
        when(encoder.matches("wrong", "HASH")).thenReturn(false);
        assertThatThrownBy(() -> login.login("ana@mail.com", "wrong")).isInstanceOf(InvalidCredentialsException.class);

        assertThatThrownBy(() -> login.login(null, "x")).isInstanceOf(InvalidCredentialsException.class);
        assertThatThrownBy(() -> login.login("ana@mail.com", null)).isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void loginRejectsInactiveAccount() {
        when(accounts.findByIdentifier("ana@mail.com")).thenReturn(Optional.of(customer(false)));
        when(encoder.matches("secret123", "HASH")).thenReturn(true);

        assertThatThrownBy(() -> new LoginUseCase(accounts, encoder).login("ana@mail.com", "secret123"))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid identifier or password");
    }
}
