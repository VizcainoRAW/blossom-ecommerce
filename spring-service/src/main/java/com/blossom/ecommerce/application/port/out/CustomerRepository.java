package com.blossom.ecommerce.application.port.out;

import com.blossom.ecommerce.domain.model.user.Customer;

public interface CustomerRepository {

    Customer save(Customer customer);

    boolean existsByIdentifier(String id);
}
