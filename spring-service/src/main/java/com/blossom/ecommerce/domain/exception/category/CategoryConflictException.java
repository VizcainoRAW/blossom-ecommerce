package com.blossom.ecommerce.domain.exception.category;

import com.blossom.ecommerce.domain.exception.DomainException;

/**
 * The category clashes with existing data: duplicate name, or deleting one that still has products.
 */
public class CategoryConflictException extends DomainException {
    public CategoryConflictException(String message) {
        super(message);
    }
}
