package com.blossom.ecommerce.application.port.out;

import com.blossom.ecommerce.domain.model.category.Category;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository {

    Category save(Category category);

    Optional<Category> findById(Long id);

    Optional<Category> findByNameIgnoreCase(String name);

    List<Category> findAll();

    boolean existsById(Long id);

    /** True if any product, active or not, still points at this category. */
    boolean hasProducts(Long id);

    void deleteById(Long id);
}
