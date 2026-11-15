package com.blossom.ecommerce.application.port.out;

import com.blossom.ecommerce.domain.model.order.Order;
import com.blossom.ecommerce.domain.model.order.OrderStatus;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository {

    /** Inserts a new order, or saves the status of an existing one (items never change after placement). */
    Order save(Order order);

    Optional<Order> findById(Long id);

    /** Same as findById, but locks the order row until the transaction ends. Call inside a transaction. */
    Optional<Order> findByIdForUpdate(Long id);

    /** Newest first. Null filters match everything. */
    PageResult<Order> search(UUID customerId, OrderStatus status, int page, int size);
}
