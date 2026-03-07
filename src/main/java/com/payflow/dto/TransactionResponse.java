package com.payflow.dto;

import com.payflow.model.Transaction;
import com.payflow.model.TransactionStatus;
import com.payflow.model.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransactionResponse(
        Long id,
        Long accountId,
        TransactionType type,
        BigDecimal amount,
        String description,
        UUID correlationId,
        TransactionStatus status,
        BigDecimal balanceAfter,
        LocalDateTime createdAt
) {
    public static TransactionResponse from(Transaction tx) {
        return new TransactionResponse(
                tx.getId(),
                tx.getAccountId(),
                tx.getType(),
                tx.getAmount(),
                tx.getDescription(),
                tx.getCorrelationId(),
                tx.getStatus(),
                tx.getBalanceAfter(),
                tx.getCreatedAt()
        );
    }
}
