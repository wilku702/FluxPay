package com.payflow.exception;

import com.payflow.model.Transaction;
import lombok.Getter;

@Getter
public class DuplicateTransactionException extends RuntimeException {
    private final Transaction existingTransaction;

    public DuplicateTransactionException(Transaction existingTransaction) {
        super("Duplicate transaction with idempotency key: " + existingTransaction.getIdempotencyKey());
        this.existingTransaction = existingTransaction;
    }
}
