package com.blossom.ecommerce.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AccountJpaRepository extends JpaRepository<AccountEntity, UUID> {

    boolean existsByIdentifier(String identifier);

    Optional<AccountEntity> findByIdentifier(String identifier);
}
