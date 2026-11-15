package com.blossom.ecommerce.infrastructure.web.dto;

import com.blossom.ecommerce.domain.model.order.Order;
import com.blossom.ecommerce.domain.model.order.OrderItem;
import com.blossom.ecommerce.domain.model.order.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(Long id, UUID customerId, OrderStatus status, BigDecimal total, String shippingAddress,
                            Instant createdAt, List<Item> items) {

    public record Item(Long variantId, String productName, String sku, String color, String size,
                       int quantity, BigDecimal unitPrice, BigDecimal subtotal) {
        static Item from(OrderItem i) {
            return new Item(i.variantId(), i.productName(), i.sku(), i.color(), i.size(),
                    i.quantity(), i.unitPrice(), i.subtotal());
        }
    }

    public static OrderResponse from(Order o) {
        return new OrderResponse(o.getId(), o.getCustomerId(), o.getStatus(), o.getTotal(), o.getShippingAddress(),
                o.getCreatedAt(), o.getItems().stream().map(Item::from).toList());
    }
}
