package com.blossom.ecommerce.application.port.in;

import com.blossom.ecommerce.application.port.out.CategoryRepository;
import com.blossom.ecommerce.domain.exception.category.CategoryConflictException;
import com.blossom.ecommerce.domain.exception.category.CategoryNotFoundException;
import com.blossom.ecommerce.domain.model.category.Category;

import java.util.List;

public class ManageCategoriesUseCase {

    private final CategoryRepository categoryRepository;

    public ManageCategoriesUseCase(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public Category create(String name) {
        Category category = Category.create(name);
        requireUniqueName(category);
        return categoryRepository.save(category);
    }

    public Category get(Long id) {
        return categoryRepository.findById(id).orElseThrow(() -> new CategoryNotFoundException(id));
    }

    public List<Category> list() {
        return categoryRepository.findAll();
    }

    public Category rename(Long id, String name) {
        Category category = get(id);
        category.rename(name);
        requireUniqueName(category);
        return categoryRepository.save(category);
    }

    /**
     * Hard delete, only allowed while no product uses the category.
     */
    public void delete(Long id) {
        get(id);
        if (categoryRepository.hasProducts(id)) {
            throw new CategoryConflictException("Category " + id + " still has products");
        }
        categoryRepository.deleteById(id);
    }

    private void requireUniqueName(Category category) {
        categoryRepository.findByNameIgnoreCase(category.getName())
                .filter(other -> !other.getId().equals(category.getId()))
                .ifPresent(other -> {
                    throw new CategoryConflictException("Category " + category.getName() + " already exists");
                });
    }
}
