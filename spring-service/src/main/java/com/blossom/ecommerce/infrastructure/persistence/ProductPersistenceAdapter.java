package com.blossom.ecommerce.infrastructure.persistence;

import com.blossom.ecommerce.application.port.in.ProductSearchCriteria;
import com.blossom.ecommerce.application.port.out.PageResult;
import com.blossom.ecommerce.application.port.out.ProductRepository;
import com.blossom.ecommerce.domain.exception.product.ProductNotFoundException;
import com.blossom.ecommerce.domain.model.product.Product;
import com.blossom.ecommerce.domain.model.product.ProductVariant;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Repository
public class ProductPersistenceAdapter implements ProductRepository {

    private final ProductJpaRepository jpa;

    public ProductPersistenceAdapter(ProductJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional
    public Product save(Product product) {
        ProductEntity entity = product.getId() == null
                ? new ProductEntity()
                : jpa.findById(product.getId()).orElseThrow(() -> new ProductNotFoundException(product.getId()));

        entity.setCategoryId(product.getCategoryId());
        entity.setName(product.getName());
        entity.setDescription(product.getDescription());
        entity.setImageUrls(new ArrayList<>(product.getImageUrls()));
        entity.setActive(product.isActive());
        syncVariants(entity, product.getVariants());

        // Flush so new variants get their generated ids before mapping back.
        return toDomain(jpa.saveAndFlush(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Product> findActiveById(Long id) {
        return jpa.findById(id).filter(ProductEntity::isActive).map(ProductPersistenceAdapter::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Product> searchActive(ProductSearchCriteria c) {
        Page<ProductEntity> page = jpa.findAll(matching(c), PageRequest.of(c.page(), c.size(), Sort.by("id")));
        return new PageResult<>(page.map(ProductPersistenceAdapter::toDomain).getContent(),
                c.page(), c.size(), page.getTotalElements());
    }

    private static Specification<ProductEntity> matching(ProductSearchCriteria c) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isTrue(root.get("active")));
            if (c.name() != null) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + escapeLike(c.name().toLowerCase()) + "%", '\\'));
            }
            if (c.categoryId() != null) {
                predicates.add(cb.equal(root.get("categoryId"), c.categoryId()));
            }
            if (c.minPrice() != null || c.maxPrice() != null) {
                Subquery<Integer> variant = query.subquery(Integer.class);
                Root<ProductVariantEntity> v = variant.from(ProductVariantEntity.class);
                List<Predicate> priceFilters = new ArrayList<>();
                priceFilters.add(cb.equal(v.get("product"), root));
                if (c.minPrice() != null) priceFilters.add(cb.ge(v.get("price"), c.minPrice()));
                if (c.maxPrice() != null) priceFilters.add(cb.le(v.get("price"), c.maxPrice()));
                variant.select(cb.literal(1)).where(priceFilters.toArray(Predicate[]::new));
                predicates.add(cb.exists(variant));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static String escapeLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    /**
     * Matches incoming variants to existing rows by SKU so their ids (and any order references) survive an update.
     * Variants missing from the update are deleted.
     */
    private static void syncVariants(ProductEntity entity, List<ProductVariant> variants) {
        Map<String, ProductVariantEntity> existing = entity.getVariants().stream()
                .collect(Collectors.toMap(ProductVariantEntity::getSku, Function.identity()));
        List<ProductVariantEntity> next = new ArrayList<>();
        for (ProductVariant v : variants) {
            ProductVariantEntity ve = existing.containsKey(v.sku()) ? existing.get(v.sku()) : new ProductVariantEntity();
            ve.setProduct(entity);
            ve.setColor(v.color());
            ve.setSize(v.size());
            ve.setSku(v.sku());
            ve.setPrice(v.price());
            ve.setStock(v.stock());
            next.add(ve);
        }
        entity.getVariants().clear();
        entity.getVariants().addAll(next);
    }

    private static Product toDomain(ProductEntity e) {
        List<ProductVariant> variants = e.getVariants().stream()
                .map(v -> new ProductVariant(v.getId(), v.getColor(), v.getSize(), v.getSku(), v.getPrice(), v.getStock()))
                .toList();
        return Product.restore(e.getId(), e.getCategoryId(), e.getName(), e.getDescription(),
                e.getImageUrls(), e.isActive(), variants);
    }
}
