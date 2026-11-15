package com.blossom.ecommerce.infrastructure.persistence;

import com.blossom.ecommerce.application.port.out.AccountRepository;
import com.blossom.ecommerce.application.port.out.CustomerRepository;
import com.blossom.ecommerce.domain.model.user.Account;
import com.blossom.ecommerce.domain.model.user.Customer;
import com.blossom.ecommerce.domain.model.user.Employee;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class AccountPersistenceAdapter implements AccountRepository, CustomerRepository {

    private final AccountJpaRepository jpa;

    public AccountPersistenceAdapter(AccountJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Customer save(Customer customer) {
        return toDomain((CustomerEntity) jpa.save(toEntity(customer)));
    }

    @Override
    public boolean existsByIdentifier(String id) {
        return jpa.existsByIdentifier(id);
    }

    @Override
    public Optional<Account> findByIdentifier(String identifier) {
        return jpa.findByIdentifier(identifier).map(e -> switch (e) {
            case CustomerEntity c -> toDomain(c);
            case EmployeeEntity emp -> toDomain(emp);
            default -> throw new IllegalStateException("Unknown account entity " + e.getClass());
        });
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

    private static Employee toDomain(EmployeeEntity e) {
        return Employee.restore(e.getId(), e.getIdentifier(), e.getPasswordHash(), e.isActive(),
                e.getFirstName(), e.getLastName(), e.getRoleId(), e.getDocumentType(), e.getDocumentNumber());
    }
}
