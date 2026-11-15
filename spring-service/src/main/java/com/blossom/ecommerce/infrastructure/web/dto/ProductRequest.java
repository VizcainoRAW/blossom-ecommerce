package com.blossom.ecommerce.infrastructure.web.dto;

import com.blossom.ecommerce.domain.model.product.ProductVariant;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

public record ProductRequest(
        @NotNull Long categoryId,
        @NotBlank @Size(max = 255) String name,
        String description,
        List<@NotBlank String> imageUrls,
        @NotEmpty List<@Valid Variant> variants) {

    public record Variant(
            @NotBlank @Size(max = 50) String color,
            @NotBlank @Size(max = 20) String size,
            @NotBlank @Size(max = 100) String sku,
            @NotNull @DecimalMin("0") @Digits(integer = 10, fraction = 2) BigDecimal price,
            @NotNull @Min(0) Integer stock) {
    }

    public List<ProductVariant> toVariants() {
        return variants.stream()
                .map(v -> new ProductVariant(null, v.color(), v.size(), v.sku(), v.price(), v.stock()))
                .toList();
    }
}
