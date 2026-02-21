package com.payflow.service;

import com.payflow.dto.*;
import com.payflow.model.Transaction;
import com.payflow.repository.AccountRepository;
import com.payflow.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final AccountRepository accountRepository;
    private final TransferExecutor transferExecutor;
    private final MetricsService metricsService;

    public TransactionResponse deposit(DepositRequest request, Long userId) {
        verifyAccountOwnership(request.getAccountId(), userId);
        return executeWithRetry(() ->
                transferExecutor.executeDeposit(
                        request.getAccountId(),
                        request.getAmount(),
                        request.getDescription(),
                        request.getIdempotencyKey()
                ));
    }

    public TransactionResponse withdraw(WithdrawRequest request, Long userId) {
        verifyAccountOwnership(request.getAccountId(), userId);
        return executeWithRetry(() ->
                transferExecutor.executeWithdraw(
                        request.getAccountId(),
                        request.getAmount(),
                        request.getDescription(),
                        request.getIdempotencyKey()
                ));
    }

    public TransferResponse transfer(TransferRequest request, Long userId) {
        if (request.getSourceAccountId().equals(request.getDestinationAccountId())) {
            throw new IllegalArgumentException("Source and destination accounts must be different");
        }

        verifyAccountOwnership(request.getSourceAccountId(), userId);
        verifyAccountOwnership(request.getDestinationAccountId(), userId);

        return executeWithRetry(() ->
                transferExecutor.executeTransfer(
                        request.getSourceAccountId(),
                        request.getDestinationAccountId(),
                        request.getAmount(),
                        request.getDescription(),
                        request.getIdempotencyKey()
                ));
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> getTransactions(Long accountId,
                                                     com.payflow.model.TransactionType type,
                                                     com.payflow.model.TransactionStatus status,
                                                     LocalDateTime from,
                                                     LocalDateTime to,
                                                     BigDecimal minAmount,
                                                     BigDecimal maxAmount,
                                                     Pageable pageable,
                                                     Long userId) {
        verifyAccountOwnership(accountId, userId);
        String typeStr = type != null ? type.name() : null;
        String statusStr = status != null ? status.name() : null;
        return transactionRepository.findByFilters(accountId, typeStr, statusStr, from, to, minAmount, maxAmount, pageable)
                .map(TransactionResponse::from);
    }

    @Transactional(readOnly = true)
    public List<Transaction> exportTransactions(Long accountId,
                                                 com.payflow.model.TransactionType type,
                                                 com.payflow.model.TransactionStatus status,
                                                 LocalDateTime from,
                                                 LocalDateTime to,
                                                 Long userId) {
        verifyAccountOwnership(accountId, userId);
        String typeStr = type != null ? type.name() : null;
        String statusStr = status != null ? status.name() : null;
        return transactionRepository.findByFilters(accountId, typeStr, statusStr, from, to, null, null,
                Pageable.unpaged(Sort.by(Sort.Direction.DESC, "created_at"))).getContent();
    }

    @Transactional(readOnly = true)
    public TransactionResponse getById(Long id, Long userId) {
        Transaction tx = transactionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + id));
        verifyAccountOwnership(tx.getAccountId(), userId);
        return TransactionResponse.from(tx);
    }

    private void verifyAccountOwnership(Long accountId, Long userId) {
        accountRepository.findById(accountId)
                .filter(account -> account.getUserId().equals(userId))
                .orElseThrow(() -> new com.payflow.exception.AccountNotFoundException(accountId));
    }

    @SuppressWarnings("unchecked")
    private <T> T executeWithRetry(java.util.function.Supplier<T> operation) {
        int attempt = 0;
        while (true) {
            try {
                T result;
                try {
                    result = metricsService.timeTransaction(operation::get);
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                metricsService.recordTransactionSuccess();
                return result;
            } catch (ObjectOptimisticLockingFailureException e) {
                attempt++;
                if (attempt >= MAX_RETRIES) {
                    log.warn("Optimistic lock failed after {} retries", MAX_RETRIES);
                    metricsService.recordTransactionFailure();
                    throw e;
                }
                log.info("Optimistic lock conflict, retrying (attempt {})", attempt);
            } catch (DataIntegrityViolationException e) {
                // Safety net: DB unique constraint caught a duplicate idempotency key
                // that slipped through the application-level check (race condition)
                log.info("DataIntegrityViolation caught — likely duplicate idempotency key, looking up original");
                return (T) lookupExistingTransaction(e);
            } catch (RuntimeException e) {
                metricsService.recordTransactionFailure();
                throw e;
            }
        }
    }

    private Object lookupExistingTransaction(DataIntegrityViolationException e) {
        String message = e.getMostSpecificCause().getMessage();
        // Extract idempotency key if possible; otherwise re-throw
        log.warn("Duplicate key constraint violation: {}", message);
        throw new IllegalStateException("Concurrent duplicate transaction detected. Please retry.", e);
    }
}
