package com.blossom.ecommerce.application.port.out;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

public interface InventoryPort {

    record OrderableVariant(Long variantId, String productName, String sku, String color, String size,
                            BigDecimal price) {
    }

    /** Variants among {@code variantIds} that belong to an active product. Unknown or hidden ones are left out. */
    List<OrderableVariant> findOrderable(Collection<Long> variantIds);

    /** Atomically takes {@code quantity} from stock. Returns false, changing nothing, if there isn't enough. */
    boolean decreaseStock(Long variantId, int quantity);

    void increaseStock(Long variantId, int quantity);
}
