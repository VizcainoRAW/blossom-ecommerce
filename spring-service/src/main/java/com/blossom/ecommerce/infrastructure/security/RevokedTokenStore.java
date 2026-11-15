package com.blossom.ecommerce.infrastructure.security;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Token ids (jti) revoked by logout, kept until the token would have expired anyway.
 */
// shortcut: in-memory, so revocations are lost on restart and not shared between instances. Move to Redis/DB when scaling out.
@Component
public class RevokedTokenStore {

    private final Map<String, Instant> revoked = new ConcurrentHashMap<>();

    public void revoke(String tokenId, Instant expiresAt) {
        revoked.values().removeIf(exp -> exp.isBefore(Instant.now()));
        revoked.put(tokenId, expiresAt);
    }

    public boolean isRevoked(String tokenId) {
        return tokenId != null && revoked.containsKey(tokenId);
    }
}
