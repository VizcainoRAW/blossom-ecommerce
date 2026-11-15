package com.blossom.ecommerce.application.port.in;

import java.util.UUID;

/**
 * The authenticated account making a request.
 */
public record Requester(UUID accountId, boolean employee) {
}
