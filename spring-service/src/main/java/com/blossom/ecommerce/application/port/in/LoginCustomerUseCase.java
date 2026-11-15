package com.blossom.ecommerce.application.port.in;

import com.blossom.ecommerce.application.port.out.CustomerRepository;
import com.blossom.ecommerce.application.port.out.PasswordEncoderPort;
import com.blossom.ecommerce.domain.exception.account.InvalidCredentialsException;
import com.blossom.ecommerce.domain.model.user.Customer;

public class LoginCustomerUseCase {

    private final CustomerRepository customerRepository;
    private final PasswordEncoderPort encoder;

    public LoginCustomerUseCase(CustomerRepository customerRepository, PasswordEncoderPort encoder) {
        this.customerRepository = customerRepository;
        this.encoder = encoder;
    }

    public Customer login(String identifier, String password) {
        if (identifier == null || password == null) throw new InvalidCredentialsException();
        return customerRepository.findByIdentifier(identifier.trim().toLowerCase())
                .filter(c -> encoder.matches(password, c.getPasswordHash()))
                .filter(Customer::isActive)
                .orElseThrow(InvalidCredentialsException::new);
    }
}
