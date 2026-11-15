package com.blossom.ecommerce.infrastructure.persistence;

import com.blossom.ecommerce.application.port.out.CustomerRepository;
import com.blossom.ecommerce.domain.model.user.Customer;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class CustomerRepositoryAdapter implements CustomerRepository {

    private final CustomerJpaRepository jpa;

    public CustomerRepositoryAdapter(CustomerJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Customer save(Customer customer) {
        return toDomain(jpa.save(toEntity(customer)));
    }

    @Override
    public boolean existsByIdentifier(String id) {
        return jpa.existsByIdentifier(id);
    }

    @Override
    public Optional<Customer> findByIdentifier(String identifier) {
        return jpa.findByIdentifier(identifier).map(CustomerRepositoryAdapter::toDomain);
    }

    private static CustomerEntity toEntity(Customer c) {
        CustomerEntity e = new CustomerEntity();
        e.setId(c.getId());
        e.setIdentifier(c.getIdentifier());
        e.setPasswordHash(c.getPasswordHash());
        e.setActive(c.isActive());
        e.setFirstName(c.getFirstName());
        e.setLastName(c.getLastName());
        e.setDocumentType(c.getDocumentType());
        e.setDocumentNumber(c.getDocumentNumber());
        return e;
    }

    private static Customer toDomain(CustomerEntity e) {
        return Customer.restore(e.getId(), e.getIdentifier(), e.getPasswordHash(), e.isActive(),
                e.getFirstName(), e.getLastName(), e.getDocumentType(), e.getDocumentNumber());
    }
}
