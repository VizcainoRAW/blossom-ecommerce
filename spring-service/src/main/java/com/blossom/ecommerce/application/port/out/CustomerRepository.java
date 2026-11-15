package com.blossom.ecommerce.application.port.out;

import com.blossom.ecommerce.domain.model.user.Customer;

public interface CustomerRepository {

    Customer save(Customer customer);

    /** Checks every account type, since identifiers are unique across customers and employees. */
    boolean existsByIdentifier(String id);
}
