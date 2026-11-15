package com.blossom.ecommerce.domain.exception.product;

import com.blossom.ecommerce.domain.exception.DomainException;

public class ProductNotFoundException extends DomainException {
    public ProductNotFoundException(Long id) {
        super("Product " + id + " not found");
    }
}
