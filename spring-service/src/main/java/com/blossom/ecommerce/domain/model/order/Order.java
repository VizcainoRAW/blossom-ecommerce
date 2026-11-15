package com.blossom.ecommerce.domain.model.order;

import com.blossom.ecommerce.domain.exception.order.InvalidOrderException;
import com.blossom.ecommerce.domain.exception.order.OrderConflictException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class Order {

    private final Long id;
    private final UUID customerId;
    private OrderStatus status;
    private final BigDecimal total;
    private final String shippingAddress;
    private final List<OrderItem> items;
    private final Instant createdAt;

    private Order(Long id, UUID customerId, OrderStatus status, BigDecimal total, String shippingAddress,
                  List<OrderItem> items, Instant createdAt) {
        this.id = id;
        this.customerId = customerId;
        this.status = status;
        this.total = total;
        this.shippingAddress = shippingAddress;
        this.items = List.copyOf(items);
        this.createdAt = createdAt;
    }

    public static Order place(UUID customerId, String shippingAddress, List<OrderItem> items) {
        if (customerId == null) throw new InvalidOrderException("Customer required");
        if (shippingAddress == null || shippingAddress.isBlank()) throw new InvalidOrderException("Shipping address required");
        if (items == null || items.isEmpty()) throw new InvalidOrderException("At least one item required");

        Set<Long> variantIds = new HashSet<>();
        for (OrderItem item : items) {
            if (!variantIds.add(item.variantId())) {
                throw new InvalidOrderException("Variant " + item.variantId() + " appears more than once");
            }
        }

        BigDecimal total = items.stream().map(OrderItem::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        return new Order(null, customerId, OrderStatus.PENDING, total, shippingAddress.trim(), items, null);
    }

    public static Order restore(Long id, UUID customerId, OrderStatus status, BigDecimal total, String shippingAddress,
                                List<OrderItem> items, Instant createdAt) {
        return new Order(id, customerId, status, total, shippingAddress, items, createdAt);
    }

    public void transitionTo(OrderStatus next) {
        if (next == null) throw new InvalidOrderException("Status required");
        if (!status.canTransitionTo(next)) {
            throw new OrderConflictException("Cannot change order " + id + " from " + status + " to " + next);
        }
        this.status = next;
    }

    public boolean belongsTo(UUID accountId) { return customerId.equals(accountId); }

    public Long getId() { return id; }
    public UUID getCustomerId() { return customerId; }
    public OrderStatus getStatus() { return status; }
    public BigDecimal getTotal() { return total; }
    public String getShippingAddress() { return shippingAddress; }
    public List<OrderItem> getItems() { return items; }
    public Instant getCreatedAt() { return createdAt; }

    @Override
    public String toString() { return "Order(%s/%s)".formatted(id, status); }
}
