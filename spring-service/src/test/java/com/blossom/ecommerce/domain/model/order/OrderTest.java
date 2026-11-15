package com.blossom.ecommerce.domain.model.order;

import com.blossom.ecommerce.domain.exception.order.InvalidOrderException;
import com.blossom.ecommerce.domain.exception.order.OrderConflictException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {

    static final UUID CUSTOMER = UUID.randomUUID();

    static OrderItem item(long variantId, int quantity, String price) {
        return new OrderItem(variantId, "Tee", "SKU-" + variantId, "Black", "M", quantity, new BigDecimal(price));
    }

    @Test
    void placeComputesTotalAndStartsPending() {
        Order order = Order.place(CUSTOMER, " Calle 1 ", List.of(item(1, 3, "19.99"), item(2, 1, "21.5")));

        assertThat(order.getId()).isNull();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getTotal()).isEqualByComparingTo("81.47");
        assertThat(order.getTotal().scale()).isEqualTo(2);
        assertThat(order.getShippingAddress()).isEqualTo("Calle 1");
        assertThat(order.getCustomerId()).isEqualTo(CUSTOMER);
        assertThat(order.getItems()).hasSize(2);
        assertThat(order.getCreatedAt()).isNull();
    }

    @Test
    void placeValidatesInput() {
        List<OrderItem> items = List.of(item(1, 1, "1"));

        assertThatThrownBy(() -> Order.place(null, "x", items)).hasMessage("Customer required");
        assertThatThrownBy(() -> Order.place(CUSTOMER, " ", items)).hasMessage("Shipping address required");
        assertThatThrownBy(() -> Order.place(CUSTOMER, null, items)).isInstanceOf(InvalidOrderException.class);
        assertThatThrownBy(() -> Order.place(CUSTOMER, "x", List.of())).hasMessage("At least one item required");
        assertThatThrownBy(() -> Order.place(CUSTOMER, "x", null)).isInstanceOf(InvalidOrderException.class);
        assertThatThrownBy(() -> Order.place(CUSTOMER, "x", List.of(item(1, 1, "1"), item(1, 2, "1"))))
                .hasMessage("Variant 1 appears more than once");
    }

    @Test
    void itemValidatesAndComputesSubtotal() {
        assertThat(item(1, 3, "2.50").subtotal()).isEqualByComparingTo("7.50");

        assertThatThrownBy(() -> new OrderItem(null, "n", "s", "c", "z", 1, BigDecimal.ONE)).hasMessage("Variant required");
        assertThatThrownBy(() -> item(1, 0, "1")).hasMessage("Quantity must be > 0");
        assertThatThrownBy(() -> item(1, 1, "-1")).hasMessage("Unit price must be >= 0");
        assertThatThrownBy(() -> new OrderItem(1L, "n", "s", "c", "z", 1, null)).hasMessage("Unit price must be >= 0");
    }

    @Test
    void transitionsFollowTheLifecycle() {
        Instant created = Instant.parse("2026-01-01T00:00:00Z");
        Order order = Order.restore(5L, CUSTOMER, OrderStatus.PENDING, BigDecimal.TEN, "x", List.of(item(1, 1, "10")), created);

        order.transitionTo(OrderStatus.PAID);
        order.transitionTo(OrderStatus.SHIPPED);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.SHIPPED);
        assertThat(order.getCreatedAt()).isEqualTo(created);
        assertThat(order.toString()).isEqualTo("Order(5/SHIPPED)");
        assertThatThrownBy(() -> order.transitionTo(OrderStatus.CANCELLED))
                .isInstanceOf(OrderConflictException.class)
                .hasMessage("Cannot change order 5 from SHIPPED to CANCELLED");
        assertThatThrownBy(() -> order.transitionTo(null)).hasMessage("Status required");
    }

    @Test
    void belongsToOnlyItsCustomer() {
        Order order = Order.place(CUSTOMER, "x", List.of(item(1, 1, "1")));

        assertThat(order.belongsTo(CUSTOMER)).isTrue();
        assertThat(order.belongsTo(UUID.randomUUID())).isFalse();
    }

    @ParameterizedTest
    @CsvSource({
            "PENDING,PAID,true", "PENDING,CANCELLED,true", "PENDING,SHIPPED,false", "PENDING,PENDING,false",
            "PAID,SHIPPED,true", "PAID,CANCELLED,true", "PAID,PENDING,false",
            "SHIPPED,CANCELLED,false", "SHIPPED,PAID,false",
            "CANCELLED,PENDING,false", "CANCELLED,PAID,false"
    })
    void statusTransitionRules(OrderStatus from, OrderStatus to, boolean allowed) {
        assertThat(from.canTransitionTo(to)).isEqualTo(allowed);
    }
}
