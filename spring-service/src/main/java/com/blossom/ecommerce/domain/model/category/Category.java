package com.blossom.ecommerce.domain.model.category;

import com.blossom.ecommerce.domain.exception.category.InvalidCategoryException;

public class Category {

    public static final int MAX_NAME_LENGTH = 150;

    private final Long id;
    private String name;

    private Category(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public static Category create(String name) {
        Category c = new Category(null, null);
        c.rename(name);
        return c;
    }

    public static Category restore(Long id, String name) {
        return new Category(id, name);
    }

    public void rename(String name) {
        if (name == null || name.isBlank()) throw new InvalidCategoryException("Name required");
        if (name.trim().length() > MAX_NAME_LENGTH) {
            throw new InvalidCategoryException("Name must be at most " + MAX_NAME_LENGTH + " characters");
        }
        this.name = name.trim();
    }

    public Long getId() { return id; }
    public String getName() { return name; }

    @Override
    public String toString() { return "Category(%s/%s)".formatted(id, name); }
}
