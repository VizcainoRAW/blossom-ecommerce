package com.blossom.ecommerce.infrastructure.web.dto;

import com.blossom.ecommerce.domain.model.order.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeOrderStatusRequest(@NotNull OrderStatus status) {
}
