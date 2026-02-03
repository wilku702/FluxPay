package com.payflow.service;

import com.payflow.dto.*;
import com.payflow.exception.DuplicateTransactionException;
import com.payflow.model.Transaction;
import com.payflow.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private static final int MAX_RETRIES = 3;

    private final TransactionRepository transactionRepository;
    private final TransferExecutor transferExecutor;

    public TransactionResponse deposit(DepositRequest request) {
        checkIdempotency(request.getIdempotencyKey());

        return executeWithRetry(() ->
                transferExecutor.executeDeposit(
                        request.getAccountId(),
                        request.getAmount(),
                        request.getDescription(),
                        request.getIdempotencyKey()
                ));
    }

    public TransactionResponse withdraw(WithdrawRequest request) {
        checkIdempotency(request.getIdempotencyKey());

        return executeWithRetry(() ->
                transferExecutor.executeWithdraw(
                        request.getAccountId(),
                        request.getAmount(),
                        request.getDescription(),
                        request.getIdempotencyKey()
                ));
    }

    public TransferResponse transfer(TransferRequest request) {
        if (request.getSourceAccountId().equals(request.getDestinationAccountId())) {
            throw new IllegalArgumentException("Source and destination accounts must be different");
        }

        // Check idempotency — look for existing debit transaction
        Optional<Transaction> existing = transactionRepository.findByIdempotencyKey(request.getIdempotencyKey());
        if (existing.isPresent()) {
            Transaction debit = existing.get();
            List<Transaction> pair = transactionRepository.findByCorrelationId(debit.getCorrelationId());
            Transaction credit = pair.stream()
                    .filter(t -> !t.getId().equals(debit.getId()))
                    .findFirst()
                    .orElse(debit);
            return new TransferResponse(debit.getCorrelationId(),
                    TransactionResponse.from(debit),
                    TransactionResponse.from(credit));
        }

        return executeWithRetry(() ->
                transferExecutor.executeTransfer(
                        request.getSourceAccountId(),
                        request.getDestinationAccountId(),
                        request.getAmount(),
                        request.getDescription(),
                        request.getIdempotencyKey()
                ));
    }

    public Page<TransactionResponse> getTransactions(Long accountId,
                                                     com.payflow.model.TransactionType type,
                                                     com.payflow.model.TransactionStatus status,
                                                     LocalDateTime from,
                                                     LocalDateTime to,
                                                     BigDecimal minAmount,
                                                     BigDecimal maxAmount,
                                                     Pageable pageable) {
        return transactionRepository.findByFilters(accountId, type, status, from, to, minAmount, maxAmount, pageable)
                .map(TransactionResponse::from);
    }

    public TransactionResponse getById(Long id) {
        Transaction tx = transactionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + id));
        return TransactionResponse.from(tx);
    }

    private void checkIdempotency(String idempotencyKey) {
        transactionRepository.findByIdempotencyKey(idempotencyKey)
                .ifPresent(tx -> { throw new DuplicateTransactionException(tx); });
    }

    @SuppressWarnings("unchecked")
    private <T> T executeWithRetry(java.util.function.Supplier<T> operation) {
        int attempt = 0;
        while (true) {
            try {
                return operation.get();
            } catch (ObjectOptimisticLockingFailureException e) {
                attempt++;
                if (attempt >= MAX_RETRIES) {
                    log.warn("Optimistic lock failed after {} retries", MAX_RETRIES);
                    throw e;
                }
                log.info("Optimistic lock conflict, retrying (attempt {})", attempt);
            }
        }
    }
}
