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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManageOrdersUseCaseTest {

    static final UUID CUSTOMER = UUID.randomUUID();
    static final Requester AS_CUSTOMER = new Requester(CUSTOMER, false);
    static final Requester AS_OTHER_CUSTOMER = new Requester(UUID.randomUUID(), false);
    static final Requester AS_EMPLOYEE = new Requester(UUID.randomUUID(), true);

    @Mock OrderRepository orders;
    @Mock InventoryPort inventory;
    ManageOrdersUseCase useCase;

    /** Runs work inline; rollback itself is covered by the integration tests. */
    final TransactionPort inline = new TransactionPort() {
        @Override
        public <T> T inTransaction(Supplier<T> work) {
            return work.get();
        }
    };

    @BeforeEach
    void setUp() {
        useCase = new ManageOrdersUseCase(orders, inventory, inline);
    }

    static OrderableVariant variant(long id, String price) {
        return new OrderableVariant(id, "Tee", "SKU-" + id, "Black", "M", new BigDecimal(price));
    }

    static Order order(OrderStatus status) {
        OrderItem item = new OrderItem(2L, "Tee", "SKU-2", "Black", "M", 3, BigDecimal.TEN);
        return Order.restore(7L, CUSTOMER, status, new BigDecimal("30.00"), "x", List.of(item), null);
    }

    @Test
    void placeSnapshotsPricesAndTakesStockInVariantOrder() {
        when(inventory.findOrderable(List.of(9L, 2L))).thenReturn(List.of(variant(2, "5.00"), variant(9, "1.50")));
        when(inventory.decreaseStock(anyLong(), anyInt())).thenReturn(true);
        when(orders.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Order placed = useCase.place(CUSTOMER, "Calle 1", List.of(new OrderLine(9L, 2), new OrderLine(2L, 1)));

        assertThat(placed.getTotal()).isEqualByComparingTo("8.00");
        assertThat(placed.getItems()).extracting(OrderItem::sku).containsExactly("SKU-9", "SKU-2");
        InOrder stock = inOrder(inventory);
        stock.verify(inventory).decreaseStock(2L, 1);
        stock.verify(inventory).decreaseStock(9L, 2);
    }

    @Test
    void placeRejectsEmptyAndUnavailableItems() {
        assertThatThrownBy(() -> useCase.place(CUSTOMER, "x", List.of())).isInstanceOf(InvalidOrderException.class);
        assertThatThrownBy(() -> useCase.place(CUSTOMER, "x", null)).isInstanceOf(InvalidOrderException.class);

        when(inventory.findOrderable(List.of(3L))).thenReturn(List.of());
        assertThatThrownBy(() -> useCase.place(CUSTOMER, "x", List.of(new OrderLine(3L, 1))))
                .isInstanceOf(InvalidOrderException.class).hasMessage("Variant 3 is not available");
        verify(orders, never()).save(any());
    }

    @Test
    void placeFailsWhenStockRunsOut() {
        when(inventory.findOrderable(List.of(2L))).thenReturn(List.of(variant(2, "5")));
        when(inventory.decreaseStock(2L, 4)).thenReturn(false);

        assertThatThrownBy(() -> useCase.place(CUSTOMER, "x", List.of(new OrderLine(2L, 4))))
                .isInstanceOf(OrderConflictException.class).hasMessage("Not enough stock for SKU-2");
        verify(orders, never()).save(any());
    }

    @Test
    void getHidesOtherCustomersOrders() {
        when(orders.findById(7L)).thenReturn(Optional.of(order(OrderStatus.PENDING)));

        assertThat(useCase.get(7L, AS_CUSTOMER).getId()).isEqualTo(7L);
        assertThat(useCase.get(7L, AS_EMPLOYEE).getId()).isEqualTo(7L);
        assertThatThrownBy(() -> useCase.get(7L, AS_OTHER_CUSTOMER)).isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void listForcesCustomerFilterButLetsEmployeesChoose() {
        UUID someone = UUID.randomUUID();
        PageResult<Order> empty = new PageResult<>(List.of(), 0, 20, 0);
        when(orders.search(CUSTOMER, null, 0, 100)).thenReturn(empty);
        when(orders.search(someone, OrderStatus.PAID, 2, 1)).thenReturn(empty);

        assertThat(useCase.list(AS_CUSTOMER, someone, null, -1, 500)).isSameAs(empty);
        assertThat(useCase.list(AS_EMPLOYEE, someone, OrderStatus.PAID, 2, 0)).isSameAs(empty);
    }

    @Test
    void customerCancelsOwnPendingOrderAndStockReturns() {
        when(orders.findByIdForUpdate(7L)).thenReturn(Optional.of(order(OrderStatus.PENDING)));
        when(orders.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThat(useCase.cancel(7L, AS_CUSTOMER).getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(inventory).increaseStock(2L, 3);
    }

    @Test
    void customerCannotCancelPaidOrderButEmployeeCan() {
        when(orders.findByIdForUpdate(7L)).thenReturn(Optional.of(order(OrderStatus.PAID)));
        when(orders.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> useCase.cancel(7L, AS_CUSTOMER))
                .isInstanceOf(OrderConflictException.class).hasMessage("Only pending orders can be cancelled");
        assertThat(useCase.cancel(7L, AS_EMPLOYEE).getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void cancelOfAnotherCustomersOrderLooksMissing() {
        when(orders.findByIdForUpdate(7L)).thenReturn(Optional.of(order(OrderStatus.PENDING)));

        assertThatThrownBy(() -> useCase.cancel(7L, AS_OTHER_CUSTOMER)).isInstanceOf(OrderNotFoundException.class);
        verify(inventory, never()).increaseStock(anyLong(), anyInt());
    }

    @Test
    void changeStatusMovesForwardOrCancelsWithRestock() {
        when(orders.findByIdForUpdate(7L))
                .thenReturn(Optional.of(order(OrderStatus.PENDING)))
                .thenReturn(Optional.of(order(OrderStatus.PAID)));
        when(orders.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThat(useCase.changeStatus(7L, OrderStatus.PAID).getStatus()).isEqualTo(OrderStatus.PAID);
        verify(inventory, never()).increaseStock(anyLong(), anyInt());

        assertThat(useCase.changeStatus(7L, OrderStatus.CANCELLED).getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(inventory).increaseStock(2L, 3);
    }

    @Test
    void changeStatusOfMissingOrderFails() {
        when(orders.findByIdForUpdate(8L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.changeStatus(8L, OrderStatus.PAID))
                .isInstanceOf(OrderNotFoundException.class).hasMessage("Order 8 not found");
    }
}
