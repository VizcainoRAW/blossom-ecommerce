package com.blossom.ecommerce.infrastructure.web.dto;

public record JwtResponse(String accessToken, String tokenType, long expiresIn) {
}
