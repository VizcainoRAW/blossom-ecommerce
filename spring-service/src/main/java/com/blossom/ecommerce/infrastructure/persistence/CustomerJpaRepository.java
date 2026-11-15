package com.blossom.ecommerce.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CustomerJpaRepository extends JpaRepository<CustomerEntity, UUID> {

    boolean existsByIdentifier(String identifier);

    Optional<CustomerEntity> findByIdentifier(String identifier);
}
