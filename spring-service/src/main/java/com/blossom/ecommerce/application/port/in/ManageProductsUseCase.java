package com.blossom.ecommerce.application.port.in;

import com.blossom.ecommerce.application.port.out.CategoryRepository;
import com.blossom.ecommerce.application.port.out.PageResult;
import com.blossom.ecommerce.application.port.out.ProductRepository;
import com.blossom.ecommerce.domain.exception.product.InvalidProductException;
import com.blossom.ecommerce.domain.exception.product.ProductNotFoundException;
import com.blossom.ecommerce.domain.model.product.Product;
import com.blossom.ecommerce.domain.model.product.ProductVariant;

import java.util.List;

public class ManageProductsUseCase {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ManageProductsUseCase(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    public Product create(Long categoryId, String name, String description,
                          List<String> imageUrls, List<ProductVariant> variants) {
        requireCategory(categoryId);
        return productRepository.save(Product.create(categoryId, name, description, imageUrls, variants));
    }

    public Product get(Long id) {
        return productRepository.findActiveById(id).orElseThrow(() -> new ProductNotFoundException(id));
    }

    public PageResult<Product> search(ProductSearchCriteria criteria) {
        return productRepository.searchActive(criteria);
    }

    public Product update(Long id, Long categoryId, String name, String description,
                          List<String> imageUrls, List<ProductVariant> variants) {
        Product product = get(id);
        requireCategory(categoryId);
        product.update(categoryId, name, description, imageUrls, variants);
        return productRepository.save(product);
    }

    /**
     * Soft delete: variants may already be referenced by orders, so the row stays and the product is hidden.
     */
    public void delete(Long id) {
        Product product = get(id);
        product.deactivate();
        productRepository.save(product);
    }

    private void requireCategory(Long categoryId) {
        if (categoryId != null && !categoryRepository.existsById(categoryId)) {
            throw new InvalidProductException("Category " + categoryId + " does not exist");
        }
    }
}
