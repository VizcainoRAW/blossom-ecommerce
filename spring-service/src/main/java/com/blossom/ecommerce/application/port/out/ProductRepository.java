package com.blossom.ecommerce.application.port.out;

import com.blossom.ecommerce.application.port.in.ProductSearchCriteria;
import com.blossom.ecommerce.domain.model.product.Product;

import java.util.Optional;

public interface ProductRepository {

    Product save(Product product);

    Optional<Product> findActiveById(Long id);

    PageResult<Product> searchActive(ProductSearchCriteria criteria);

}
