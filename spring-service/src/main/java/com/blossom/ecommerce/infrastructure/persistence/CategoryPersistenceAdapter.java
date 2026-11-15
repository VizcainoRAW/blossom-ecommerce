package com.blossom.ecommerce.infrastructure.persistence;

import com.blossom.ecommerce.application.port.out.CategoryRepository;
import com.blossom.ecommerce.domain.model.category.Category;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class CategoryPersistenceAdapter implements CategoryRepository {

    private final CategoryJpaRepository jpa;
    private final ProductJpaRepository products;

    public CategoryPersistenceAdapter(CategoryJpaRepository jpa, ProductJpaRepository products) {
        this.jpa = jpa;
        this.products = products;
    }

    @Override
    public Category save(Category category) {
        CategoryEntity e = new CategoryEntity();
        e.setId(category.getId());
        e.setName(category.getName());
        return toDomain(jpa.save(e));
    }

    @Override
    public Optional<Category> findById(Long id) {
        return jpa.findById(id).map(CategoryPersistenceAdapter::toDomain);
    }

    @Override
    public Optional<Category> findByNameIgnoreCase(String name) {
        return jpa.findByNameIgnoreCase(name).map(CategoryPersistenceAdapter::toDomain);
    }

    @Override
    public List<Category> findAll() {
        return jpa.findAll(Sort.by("name")).stream().map(CategoryPersistenceAdapter::toDomain).toList();
    }

    @Override
    public boolean existsById(Long id) {
        return jpa.existsById(id);
    }

    @Override
    public boolean hasProducts(Long id) {
        return products.existsByCategoryId(id);
    }

    @Override
    public void deleteById(Long id) {
        jpa.deleteById(id);
    }

    private static Category toDomain(CategoryEntity e) {
        return Category.restore(e.getId(), e.getName());
    }
}
