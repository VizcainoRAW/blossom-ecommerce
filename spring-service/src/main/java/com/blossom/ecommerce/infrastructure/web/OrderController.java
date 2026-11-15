package com.blossom.ecommerce.infrastructure.web;

import com.blossom.ecommerce.application.port.in.ManageOrdersUseCase;
import com.blossom.ecommerce.application.port.in.Requester;
import com.blossom.ecommerce.application.port.out.PageResult;
import com.blossom.ecommerce.domain.model.order.OrderStatus;
import com.blossom.ecommerce.domain.model.user.valueobject.AccountType;
import com.blossom.ecommerce.infrastructure.security.JwtTokenService;
import com.blossom.ecommerce.infrastructure.web.dto.ChangeOrderStatusRequest;
import com.blossom.ecommerce.infrastructure.web.dto.OrderResponse;
import com.blossom.ecommerce.infrastructure.web.dto.PlaceOrderRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final ManageOrdersUseCase orders;

    public OrderController(ManageOrdersUseCase orders) {
        this.orders = orders;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse place(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody PlaceOrderRequest request) {
        return OrderResponse.from(orders.place(requester(jwt).accountId(), request.shippingAddress(), request.toLines()));
    }

    /** Customers get their own orders; employees get all orders and can filter by customerId. */
    @GetMapping
    public PageResult<OrderResponse> list(@AuthenticationPrincipal Jwt jwt,
                                          @RequestParam(required = false) UUID customerId,
                                          @RequestParam(required = false) OrderStatus status,
                                          @RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "20") int size) {
        return orders.list(requester(jwt), customerId, status, page, size).map(OrderResponse::from);
    }

    @GetMapping("/{id}")
    public OrderResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return OrderResponse.from(orders.get(id, requester(jwt)));
    }

    @PostMapping("/{id}/cancel")
    public OrderResponse cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return OrderResponse.from(orders.cancel(id, requester(jwt)));
    }

    @PatchMapping("/{id}/status")
    public OrderResponse changeStatus(@PathVariable Long id, @Valid @RequestBody ChangeOrderStatusRequest request) {
        return OrderResponse.from(orders.changeStatus(id, request.status()));
    }

    private static Requester requester(Jwt jwt) {
        boolean employee = AccountType.EMPLOYEE.name().equals(jwt.getClaimAsString(JwtTokenService.ACCOUNT_TYPE_CLAIM));
        return new Requester(UUID.fromString(jwt.getSubject()), employee);
    }
}
