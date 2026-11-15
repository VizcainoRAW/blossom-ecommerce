package com.blossom.ecommerce.application.port.in;

import com.blossom.ecommerce.application.port.out.CustomerRepository;
import com.blossom.ecommerce.application.port.out.PasswordEncoderPort;
import com.blossom.ecommerce.domain.exception.account.IdentifierAlreadyRegisteredException;
import com.blossom.ecommerce.domain.model.user.Customer;
import com.blossom.ecommerce.domain.model.user.valueobject.DocumentType;

public class RegisterCustomerUseCase {

    private final CustomerRepository customerRepository;
    private final PasswordEncoderPort encoder;

    public RegisterCustomerUseCase(CustomerRepository customerRepository, PasswordEncoderPort encoder) {
        this.customerRepository = customerRepository;
        this.encoder = encoder;
    }

    public Customer registerCustomer(String name, String lastName, String identifier, String password,
                                 DocumentType documentType, String documentNumber) {
        if(customerRepository.existsByIdentifier(identifier)){
            throw new IdentifierAlreadyRegisteredException(identifier);
        }

        String passwordHash = encoder.encode(password);

        Customer customer = Customer.register(
                identifier,
                passwordHash,
                name,
                lastName,
                documentType,
                documentNumber);

        return customerRepository.save(customer);
    }
}
