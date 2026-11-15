package com.blossom.ecommerce.application.port.in;

import com.blossom.ecommerce.application.port.out.InventoryPort;
import com.blossom.ecommerce.application.port.out.InventoryPort.OrderableVariant;
import com.blossom.ecommerce.application.port.out.OrderRepository;
import com.blossom.ecommerce.application.port.out.PageResult;
import com.blossom.ecommerce.application.port.out.TransactionPort;
import com.blossom.ecommerce.domain.exception.order.InvalidOrderException;
import com.blossom.ecommerce.domain.exception.order.OrderConflictException;
import com.blossom.ecommerce.domain.exception.order.OrderNotFoundException;
import com.blossom.ecommerce.domain.model.order.Order;
import com.blossom.ecommerce.domain.model.order.OrderItem;
import com.blossom.ecommerce.domain.model.order.OrderStatus;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ManageOrdersUseCase {

    private final OrderRepository orderRepository;
    private final InventoryPort inventory;
    private final TransactionPort transactions;

    public ManageOrdersUseCase(OrderRepository orderRepository, InventoryPort inventory, TransactionPort transactions) {
        this.orderRepository = orderRepository;
        this.inventory = inventory;
        this.transactions = transactions;
    }

    public Order place(UUID customerId, String shippingAddress, List<OrderLine> lines) {
        if (lines == null || lines.isEmpty()) throw new InvalidOrderException("At least one item required");

        return transactions.inTransaction(() -> {
            Map<Long, OrderableVariant> variants = inventory.findOrderable(lines.stream().map(OrderLine::variantId).toList())
                    .stream().collect(Collectors.toMap(OrderableVariant::variantId, Function.identity()));

            List<OrderItem> items = lines.stream().map(line -> {
                OrderableVariant v = variants.get(line.variantId());
                if (v == null) throw new InvalidOrderException("Variant " + line.variantId() + " is not available");
                return new OrderItem(v.variantId(), v.productName(), v.sku(), v.color(), v.size(),
                        line.quantity(), v.price());
            }).toList();

            Order order = Order.place(customerId, shippingAddress, items);

            // Lock rows in a fixed order so two concurrent checkouts can't deadlock on each other.
            items.stream().sorted(Comparator.comparing(OrderItem::variantId)).forEach(item -> {
                if (!inventory.decreaseStock(item.variantId(), item.quantity())) {
                    throw new OrderConflictException("Not enough stock for " + item.sku());
                }
            });

            return orderRepository.save(order);
        });
    }

    public Order get(Long id, Requester requester) {
        return orderRepository.findById(id)
                .filter(o -> requester.employee() || o.belongsTo(requester.accountId()))
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    /**
     * Customers only ever see their own orders; employees see all and may filter by customer.
     */
    public PageResult<Order> list(Requester requester, UUID customerId, OrderStatus status, int page, int size) {
        UUID customerFilter = requester.employee() ? customerId : requester.accountId();
        return orderRepository.search(customerFilter, status, Math.max(page, 0), Math.clamp(size, 1, 100));
    }

    /**
     * Customers can cancel their own orders while pending; employees can also cancel paid ones.
     */
    public Order cancel(Long id, Requester requester) {
        return transactions.inTransaction(() -> {
            Order order = orderRepository.findByIdForUpdate(id)
                    .filter(o -> requester.employee() || o.belongsTo(requester.accountId()))
                    .orElseThrow(() -> new OrderNotFoundException(id));
            if (!requester.employee() && order.getStatus() != OrderStatus.PENDING) {
                throw new OrderConflictException("Only pending orders can be cancelled");
            }
            return cancelAndRestock(order);
        });
    }

    /** Employee-only status changes, e.g. PENDING -> PAID -> SHIPPED. */
    public Order changeStatus(Long id, OrderStatus status) {
        return transactions.inTransaction(() -> {
            Order order = orderRepository.findByIdForUpdate(id).orElseThrow(() -> new OrderNotFoundException(id));
            if (status == OrderStatus.CANCELLED) return cancelAndRestock(order);
            order.transitionTo(status);
            return orderRepository.save(order);
        });
    }

    private Order cancelAndRestock(Order order) {
        order.transitionTo(OrderStatus.CANCELLED);
        order.getItems().stream().sorted(Comparator.comparing(OrderItem::variantId))
                .forEach(item -> inventory.increaseStock(item.variantId(), item.quantity()));
        return orderRepository.save(order);
    }
}
