package com.payflow.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransactionEvent(
        Long transactionId,
        Long accountId,
        EventType eventType,
        TransactionEventType transactionType,
        BigDecimal amount,
        BigDecimal balanceAfter,
        UUID correlationId,
        LocalDateTime timestamp
) {
    public enum EventType {
        DEPOSIT, WITHDRAWAL, TRANSFER_DEBIT, TRANSFER_CREDIT
    }

    public enum TransactionEventType {
        CREDIT, DEBIT
    }
}
