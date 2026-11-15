package com.blossom.ecommerce.domain.exception.category;

import com.blossom.ecommerce.domain.exception.DomainException;

public class InvalidCategoryException extends DomainException {
    public InvalidCategoryException(String message) {
        super(message);
    }
}
