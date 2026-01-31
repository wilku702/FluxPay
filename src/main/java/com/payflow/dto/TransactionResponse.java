package com.payflow.dto;

import com.payflow.model.Transaction;
import com.payflow.model.TransactionStatus;
import com.payflow.model.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class TransactionResponse {
    private Long id;
    private Long accountId;
    private TransactionType type;
    private BigDecimal amount;
    private String description;
    private UUID correlationId;
    private TransactionStatus status;
    private BigDecimal balanceAfter;
    private LocalDateTime createdAt;

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
