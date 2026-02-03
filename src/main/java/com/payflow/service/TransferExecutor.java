package com.payflow.service;

import com.payflow.dto.TransferResponse;
import com.payflow.dto.TransactionResponse;
import com.payflow.exception.AccountFrozenException;
import com.payflow.exception.AccountNotFoundException;
import com.payflow.exception.InsufficientFundsException;
import com.payflow.model.*;
import com.payflow.repository.AccountRepository;
import com.payflow.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TransferExecutor {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TransferResponse executeTransfer(Long sourceId, Long destId, BigDecimal amount,
                                            String description, String idempotencyKey) {
        Account source = accountRepository.findById(sourceId)
                .orElseThrow(() -> new AccountNotFoundException(sourceId));
        Account dest = accountRepository.findById(destId)
                .orElseThrow(() -> new AccountNotFoundException(destId));

        if (source.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountFrozenException(sourceId);
        }
        if (dest.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountFrozenException(destId);
        }
        if (source.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException(source.getBalance(), amount);
        }

        UUID correlationId = UUID.randomUUID();

        // Debit source
        source.setBalance(source.getBalance().subtract(amount));
        accountRepository.save(source);

        Transaction debit = new Transaction();
        debit.setAccountId(sourceId);
        debit.setType(TransactionType.DEBIT);
        debit.setAmount(amount);
        debit.setDescription(description);
        debit.setCorrelationId(correlationId);
        debit.setIdempotencyKey(idempotencyKey);
        debit.setStatus(TransactionStatus.COMPLETED);
        debit.setBalanceAfter(source.getBalance());
        debit = transactionRepository.save(debit);

        // Credit destination
        dest.setBalance(dest.getBalance().add(amount));
        accountRepository.save(dest);

        Transaction credit = new Transaction();
        credit.setAccountId(destId);
        credit.setType(TransactionType.CREDIT);
        credit.setAmount(amount);
        credit.setDescription(description);
        credit.setCorrelationId(correlationId);
        credit.setIdempotencyKey(idempotencyKey + ":C");
        credit.setStatus(TransactionStatus.COMPLETED);
        credit.setBalanceAfter(dest.getBalance());
        credit = transactionRepository.save(credit);

        return new TransferResponse(correlationId,
                TransactionResponse.from(debit),
                TransactionResponse.from(credit));
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TransactionResponse executeDeposit(Long accountId, BigDecimal amount,
                                              String description, String idempotencyKey) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountFrozenException(accountId);
        }

        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);

        Transaction tx = new Transaction();
        tx.setAccountId(accountId);
        tx.setType(TransactionType.CREDIT);
        tx.setAmount(amount);
        tx.setDescription(description != null ? description : "Deposit");
        tx.setIdempotencyKey(idempotencyKey);
        tx.setStatus(TransactionStatus.COMPLETED);
        tx.setBalanceAfter(account.getBalance());
        tx = transactionRepository.save(tx);

        return TransactionResponse.from(tx);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TransactionResponse executeWithdraw(Long accountId, BigDecimal amount,
                                               String description, String idempotencyKey) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountFrozenException(accountId);
        }
        if (account.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException(account.getBalance(), amount);
        }

        account.setBalance(account.getBalance().subtract(amount));
        accountRepository.save(account);

        Transaction tx = new Transaction();
        tx.setAccountId(accountId);
        tx.setType(TransactionType.DEBIT);
        tx.setAmount(amount);
        tx.setDescription(description != null ? description : "Withdrawal");
        tx.setIdempotencyKey(idempotencyKey);
        tx.setStatus(TransactionStatus.COMPLETED);
        tx.setBalanceAfter(account.getBalance());
        tx = transactionRepository.save(tx);

        return TransactionResponse.from(tx);
    }
}
