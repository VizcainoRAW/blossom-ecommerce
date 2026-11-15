package com.blossom.ecommerce.infrastructure.web.dto;

import com.blossom.ecommerce.domain.model.category.Category;

public record CategoryResponse(Long id, String name) {

    public static CategoryResponse from(Category c) {
        return new CategoryResponse(c.getId(), c.getName());
    }
}
