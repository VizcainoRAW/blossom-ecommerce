package com.blossom.ecommerce.infrastructure.persistence;

import com.blossom.ecommerce.application.port.out.InventoryPort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

@Repository
public class InventoryAdapter implements InventoryPort {

    private final ProductVariantJpaRepository variants;

    public InventoryAdapter(ProductVariantJpaRepository variants) {
        this.variants = variants;
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderableVariant> findOrderable(Collection<Long> variantIds) {
        return variants.findOrderable(variantIds).stream()
                .map(v -> new OrderableVariant(v.getId(), v.getProduct().getName(), v.getSku(), v.getColor(),
                        v.getSize(), v.getPrice()))
                .toList();
    }

    @Override
    @Transactional
    public boolean decreaseStock(Long variantId, int quantity) {
        return variants.decreaseStock(variantId, quantity) == 1;
    }

    @Override
    @Transactional
    public void increaseStock(Long variantId, int quantity) {
        variants.increaseStock(variantId, quantity);
    }
}
