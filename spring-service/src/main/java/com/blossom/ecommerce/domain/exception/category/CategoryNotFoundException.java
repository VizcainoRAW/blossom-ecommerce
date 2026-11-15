package com.blossom.ecommerce.domain.exception.category;

import com.blossom.ecommerce.domain.exception.DomainException;

public class CategoryNotFoundException extends DomainException {
    public CategoryNotFoundException(Long id) {
        super("Category " + id + " not found");
    }
}
