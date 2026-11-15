package com.blossom.ecommerce.application.port.out;

import java.util.function.Supplier;

public interface TransactionPort {

    /** Runs the work in one transaction; any exception rolls everything back. */
    <T> T inTransaction(Supplier<T> work);
}
