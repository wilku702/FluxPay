package com.payflow.service;

import com.payflow.dto.TransferResponse;
import com.payflow.dto.TransactionResponse;
import com.payflow.exception.AccountFrozenException;
import com.payflow.exception.AccountNotFoundException;
import com.payflow.exception.CurrencyMismatchException;
import com.payflow.exception.InsufficientFundsException;
import com.payflow.model.*;
import com.payflow.repository.AccountRepository;
import com.payflow.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransferExecutor {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TransferResponse executeTransfer(Long sourceId, Long destId, BigDecimal amount,
                                            String description, String idempotencyKey) {
        // Idempotency check inside transaction boundary
        Optional<Transaction> existing = transactionRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            Transaction debit = existing.get();
            List<Transaction> pair = transactionRepository.findByCorrelationId(debit.getCorrelationId());
            Transaction credit = pair.stream()
                    .filter(t -> !t.getId().equals(debit.getId()))
                    .findFirst()
                    .orElse(debit);
            log.info("Idempotent replay for transfer idempotencyKey={}, correlationId={}",
                    idempotencyKey, debit.getCorrelationId());
            return new TransferResponse(debit.getCorrelationId(),
                    TransactionResponse.from(debit),
                    TransactionResponse.from(credit));
        }

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
        if (!source.getCurrency().equals(dest.getCurrency())) {
            throw new CurrencyMismatchException(source.getCurrency(), dest.getCurrency());
        }
        if (source.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException();
        }

        UUID correlationId = UUID.randomUUID();

        // Debit source
        source.setBalance(source.getBalance().subtract(amount).setScale(4, RoundingMode.HALF_UP));
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

        log.info("Transfer debit: accountId={}, amount={}, balanceAfter={}, correlationId={}",
                sourceId, amount, source.getBalance(), correlationId);

        // Credit destination
        dest.setBalance(dest.getBalance().add(amount).setScale(4, RoundingMode.HALF_UP));
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

        log.info("Transfer credit: accountId={}, amount={}, balanceAfter={}, correlationId={}",
                destId, amount, dest.getBalance(), correlationId);

        return new TransferResponse(correlationId,
                TransactionResponse.from(debit),
                TransactionResponse.from(credit));
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TransactionResponse executeDeposit(Long accountId, BigDecimal amount,
                                              String description, String idempotencyKey) {
        // Idempotency check inside transaction boundary
        Optional<Transaction> existing = transactionRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            log.info("Idempotent replay for deposit idempotencyKey={}", idempotencyKey);
            return TransactionResponse.from(existing.get());
        }

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountFrozenException(accountId);
        }

        account.setBalance(account.getBalance().add(amount).setScale(4, RoundingMode.HALF_UP));
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

        log.info("Deposit: accountId={}, amount={}, balanceAfter={}", accountId, amount, account.getBalance());

        return TransactionResponse.from(tx);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TransactionResponse executeWithdraw(Long accountId, BigDecimal amount,
                                               String description, String idempotencyKey) {
        // Idempotency check inside transaction boundary
        Optional<Transaction> existing = transactionRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            log.info("Idempotent replay for withdrawal idempotencyKey={}", idempotencyKey);
            return TransactionResponse.from(existing.get());
        }

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountFrozenException(accountId);
        }
        if (account.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException();
        }

        account.setBalance(account.getBalance().subtract(amount).setScale(4, RoundingMode.HALF_UP));
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

        log.info("Withdrawal: accountId={}, amount={}, balanceAfter={}", accountId, amount, account.getBalance());

        return TransactionResponse.from(tx);
    }
}
