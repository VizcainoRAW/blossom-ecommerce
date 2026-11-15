package com.blossom.ecommerce.infrastructure.security;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class RevokedTokenStoreTest {

    final RevokedTokenStore store = new RevokedTokenStore();

    @Test
    void revokedTokensAreRemembered() {
        store.revoke("t1", Instant.now().plusSeconds(60));

        assertThat(store.isRevoked("t1")).isTrue();
        assertThat(store.isRevoked("t2")).isFalse();
        assertThat(store.isRevoked(null)).isFalse();
    }

    @Test
    void expiredEntriesArePurgedOnNextRevoke() {
        store.revoke("old", Instant.now().minusSeconds(1));
        store.revoke("new", Instant.now().plusSeconds(60));

        assertThat(store.isRevoked("old")).isFalse();
        assertThat(store.isRevoked("new")).isTrue();
    }
}
