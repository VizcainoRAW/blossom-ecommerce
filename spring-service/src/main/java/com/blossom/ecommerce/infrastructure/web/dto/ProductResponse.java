package com.blossom.ecommerce.infrastructure.web.dto;

import com.blossom.ecommerce.domain.model.product.Product;
import com.blossom.ecommerce.domain.model.product.ProductVariant;

import java.math.BigDecimal;
import java.util.List;

public record ProductResponse(Long id, Long categoryId, String name, String description,
                              List<String> imageUrls, List<Variant> variants) {

    public record Variant(Long id, String color, String size, String sku, BigDecimal price, int stock) {
        static Variant from(ProductVariant v) {
            return new Variant(v.id(), v.color(), v.size(), v.sku(), v.price(), v.stock());
        }
    }

    public static ProductResponse from(Product p) {
        return new ProductResponse(p.getId(), p.getCategoryId(), p.getName(), p.getDescription(),
                p.getImageUrls(), p.getVariants().stream().map(Variant::from).toList());
    }
}
