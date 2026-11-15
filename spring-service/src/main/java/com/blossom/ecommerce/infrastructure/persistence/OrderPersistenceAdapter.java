package com.blossom.ecommerce.infrastructure.persistence;

import com.blossom.ecommerce.application.port.out.OrderRepository;
import com.blossom.ecommerce.application.port.out.PageResult;
import com.blossom.ecommerce.domain.exception.order.OrderNotFoundException;
import com.blossom.ecommerce.domain.model.order.Order;
import com.blossom.ecommerce.domain.model.order.OrderItem;
import com.blossom.ecommerce.domain.model.order.OrderStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class OrderPersistenceAdapter implements OrderRepository {

    private final OrderJpaRepository jpa;

    public OrderPersistenceAdapter(OrderJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional
    public Order save(Order order) {
        if (order.getId() != null) {
            OrderEntity entity = jpa.findById(order.getId()).orElseThrow(() -> new OrderNotFoundException(order.getId()));
            entity.setStatus(order.getStatus());
            return toDomain(jpa.saveAndFlush(entity));
        }

        OrderEntity entity = new OrderEntity();
        entity.setCustomerId(order.getCustomerId());
        entity.setStatus(order.getStatus());
        entity.setTotal(order.getTotal());
        entity.setShippingAddress(order.getShippingAddress());
        for (OrderItem item : order.getItems()) {
            OrderItemEntity ie = new OrderItemEntity();
            ie.getId().setVariantId(item.variantId());
            ie.setOrder(entity);
            ie.setProductName(item.productName());
            ie.setSku(item.sku());
            ie.setColor(item.color());
            ie.setSize(item.size());
            ie.setQuantity(item.quantity());
            ie.setUnitPrice(item.unitPrice());
            entity.getItems().add(ie);
        }
        return toDomain(jpa.saveAndFlush(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Order> findById(Long id) {
        return jpa.findById(id).map(OrderPersistenceAdapter::toDomain);
    }

    @Override
    @Transactional
    public Optional<Order> findByIdForUpdate(Long id) {
        return jpa.findByIdForUpdate(id).map(OrderPersistenceAdapter::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Order> search(UUID customerId, OrderStatus status, int page, int size) {
        Specification<OrderEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (customerId != null) predicates.add(cb.equal(root.get("customerId"), customerId));
            if (status != null) predicates.add(cb.equal(root.get("status"), status));
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        Page<OrderEntity> result = jpa.findAll(spec, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id")));
        return new PageResult<>(result.map(OrderPersistenceAdapter::toDomain).getContent(),
                page, size, result.getTotalElements());
    }

    private static Order toDomain(OrderEntity e) {
        List<OrderItem> items = e.getItems().stream()
                .map(i -> new OrderItem(i.getId().getVariantId(), i.getProductName(), i.getSku(), i.getColor(),
                        i.getSize(), i.getQuantity(), i.getUnitPrice()))
                .toList();
        return Order.restore(e.getId(), e.getCustomerId(), e.getStatus(), e.getTotal(), e.getShippingAddress(),
                items, e.getCreateAt());
    }
}
