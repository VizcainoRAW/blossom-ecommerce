package com.blossom.ecommerce.domain.model.product;

import com.blossom.ecommerce.domain.exception.product.InvalidProductException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Product {

    private final Long id;
    private Long categoryId;
    private String name;
    private String description;
    private List<String> imageUrls;
    private boolean active;
    private List<ProductVariant> variants;

    private Product(Long id, boolean active) {
        this.id = id;
        this.active = active;
    }

    public static Product create(Long categoryId, String name, String description,
                                 List<String> imageUrls, List<ProductVariant> variants) {
        Product p = new Product(null, true);
        p.update(categoryId, name, description, imageUrls, variants);
        return p;
    }

    public static Product restore(Long id, Long categoryId, String name, String description,
                                  List<String> imageUrls, boolean active, List<ProductVariant> variants) {
        Product p = new Product(id, active);
        p.categoryId = categoryId;
        p.name = name;
        p.description = description;
        p.imageUrls = List.copyOf(imageUrls);
        p.variants = List.copyOf(variants);
        return p;
    }

    /**
     * Replaces the product's details and variants. Variants are matched to existing ones by SKU when persisted.
     */
    public void update(Long categoryId, String name, String description,
                       List<String> imageUrls, List<ProductVariant> variants) {
        if (categoryId == null)              throw new InvalidProductException("Category required");
        if (name == null || name.isBlank())  throw new InvalidProductException("Name required");
        if (variants == null || variants.isEmpty()) throw new InvalidProductException("At least one variant required");

        Set<String> skus = new HashSet<>();
        Set<String> combos = new HashSet<>();
        for (ProductVariant v : variants) {
            if (!skus.add(v.sku())) throw new InvalidProductException("Duplicate SKU " + v.sku());
            if (!combos.add(v.color().toLowerCase() + "/" + v.size().toLowerCase())) {
                throw new InvalidProductException("Duplicate variant " + v.color() + "/" + v.size());
            }
        }

        this.categoryId = categoryId;
        this.name = name.trim();
        this.description = description;
        this.imageUrls = imageUrls == null ? List.of() : List.copyOf(imageUrls);
        this.variants = List.copyOf(variants);
    }

    public void deactivate() { this.active = false; }

    public Long getId() { return id; }
    public Long getCategoryId() { return categoryId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public List<String> getImageUrls() { return imageUrls; }
    public boolean isActive() { return active; }
    public List<ProductVariant> getVariants() { return variants; }

    @Override
    public String toString() { return "Product(%s/%s)".formatted(id, name); }
}
