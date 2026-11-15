package com.blossom.ecommerce.infrastructure.security;

import com.blossom.ecommerce.domain.model.user.Account;
import com.blossom.ecommerce.infrastructure.web.dto.JwtResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class JwtTokenService {

    public static final String USER_ID_CLAIM = "user_id";
    public static final String ACCOUNT_TYPE_CLAIM = "account_type";

    private final JwtEncoder encoder;
    private final Duration expiration;

    public JwtTokenService(JwtEncoder encoder, @Value("${security.jwt.expiration}") Duration expiration) {
        this.encoder = encoder;
        this.expiration = expiration;
    }

    public JwtResponse issue(Account account) {
        Instant now = Instant.now();
        String userId = account.getId().toString();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .id(UUID.randomUUID().toString())
                .subject(userId)
                .claim(USER_ID_CLAIM, userId)
                .claim(ACCOUNT_TYPE_CLAIM, account.getAccountType().name())
                .issuedAt(now)
                .expiresAt(now.plus(expiration))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new JwtResponse(token, "Bearer", expiration.toSeconds());
    }
}
