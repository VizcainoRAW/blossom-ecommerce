package com.blossom.ecommerce.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(name = "order_items")
@Getter
@Setter
public class OrderItemEntity {

    @Embeddable
    @Getter
    @Setter
    public static class Key implements Serializable {
        @Column(name = "order_id")
        private Long orderId;

        @Column(name = "variant_id")
        private Long variantId;

        @Override
        public boolean equals(Object o) {
            return o instanceof Key other
                    && Objects.equals(orderId, other.orderId)
                    && Objects.equals(variantId, other.variantId);
        }

        @Override
        public int hashCode() { return Objects.hash(orderId, variantId); }
    }

    @EmbeddedId
    private Key id = new Key();

    @MapsId("orderId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    private OrderEntity order;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(nullable = false)
    private String sku;

    @Column(nullable = false)
    private String color;

    @Column(nullable = false)
    private String size;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;
}
