package com.blossom.ecommerce.infrastructure.web.dto;

import com.blossom.ecommerce.domain.model.user.valueobject.DocumentType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank String identifier,
        @NotBlank @Size(min = 8, max = 72) String password,
        DocumentType documentType,
        String documentNumber) {
}
