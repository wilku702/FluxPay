package com.payflow.service;

import com.payflow.dto.*;
import com.payflow.exception.AccountNotFoundException;
import com.payflow.exception.AccountFrozenException;
import com.payflow.exception.InsufficientFundsException;
import com.payflow.model.*;
import com.payflow.repository.AccountRepository;
import com.payflow.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private TransferExecutor transferExecutor;

    @InjectMocks
    private TransactionService transactionService;

    private TransferRequest transferRequest;
    private DepositRequest depositRequest;
    private WithdrawRequest withdrawRequest;
    private TransferResponse mockTransferResponse;
    private TransactionResponse mockTxResponse;
    private Account sourceAccount;
    private Account destAccount;

    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        transferRequest = new TransferRequest(1L, 2L, BigDecimal.valueOf(100), "Test transfer", "idem-key-1");
        depositRequest = new DepositRequest(1L, BigDecimal.valueOf(500), "Test deposit", "idem-dep-1");
        withdrawRequest = new WithdrawRequest(1L, BigDecimal.valueOf(50), "Test withdraw", "idem-wd-1");

        UUID corrId = UUID.randomUUID();
        TransactionResponse debitResp = new TransactionResponse(1L, 1L, TransactionType.DEBIT,
                BigDecimal.valueOf(100), "Test", corrId, TransactionStatus.COMPLETED, BigDecimal.valueOf(900), null);
        TransactionResponse creditResp = new TransactionResponse(2L, 2L, TransactionType.CREDIT,
                BigDecimal.valueOf(100), "Test", corrId, TransactionStatus.COMPLETED, BigDecimal.valueOf(1100), null);
        mockTransferResponse = new TransferResponse(corrId, debitResp, creditResp);

        mockTxResponse = new TransactionResponse(1L, 1L, TransactionType.CREDIT,
                BigDecimal.valueOf(500), "Deposit", null, TransactionStatus.COMPLETED, BigDecimal.valueOf(1500), null);

        sourceAccount = new Account(USER_ID, "Source", "USD");
        sourceAccount.setId(1L);
        sourceAccount.setBalance(BigDecimal.valueOf(1000));

        destAccount = new Account(USER_ID, "Dest", "USD");
        destAccount.setId(2L);
        destAccount.setBalance(BigDecimal.valueOf(1000));
    }

    @Test
    void transferSuccessfully() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(destAccount));
        when(transferExecutor.executeTransfer(eq(1L), eq(2L), eq(BigDecimal.valueOf(100)),
                eq("Test transfer"), eq("idem-key-1"))).thenReturn(mockTransferResponse);

        TransferResponse result = transactionService.transfer(transferRequest, USER_ID);

        assertThat(result.getDebit().getType()).isEqualTo(TransactionType.DEBIT);
        assertThat(result.getCredit().getType()).isEqualTo(TransactionType.CREDIT);
        assertThat(result.getDebit().getAmount()).isEqualByComparingTo(BigDecimal.valueOf(100));
    }

    @Test
    void transferThrowsOnSelfTransfer() {
        TransferRequest selfTransfer = new TransferRequest(1L, 1L, BigDecimal.valueOf(100), "Self", "key");

        assertThatThrownBy(() -> transactionService.transfer(selfTransfer, USER_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("different");
    }

    @Test
    void transferThrowsWhenUserDoesNotOwnSource() {
        Account otherUsersAccount = new Account(999L, "Other", "USD");
        otherUsersAccount.setId(1L);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(otherUsersAccount));

        assertThatThrownBy(() -> transactionService.transfer(transferRequest, USER_ID))
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void transferRetriesOnOptimisticLock() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(destAccount));
        when(transferExecutor.executeTransfer(any(), any(), any(), any(), any()))
                .thenThrow(new ObjectOptimisticLockingFailureException(Account.class.getName(), 1L))
                .thenReturn(mockTransferResponse);

        TransferResponse result = transactionService.transfer(transferRequest, USER_ID);

        assertThat(result).isNotNull();
        verify(transferExecutor, times(2)).executeTransfer(any(), any(), any(), any(), any());
    }

    @Test
    void transferThrowsAfterMaxRetries() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(destAccount));
        when(transferExecutor.executeTransfer(any(), any(), any(), any(), any()))
                .thenThrow(new ObjectOptimisticLockingFailureException(Account.class.getName(), 1L));

        assertThatThrownBy(() -> transactionService.transfer(transferRequest, USER_ID))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
        verify(transferExecutor, times(3)).executeTransfer(any(), any(), any(), any(), any());
    }

    @Test
    void transferThrowsOnInsufficientFunds() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(destAccount));
        when(transferExecutor.executeTransfer(any(), any(), any(), any(), any()))
                .thenThrow(new InsufficientFundsException(BigDecimal.valueOf(50), BigDecimal.valueOf(100)));

        assertThatThrownBy(() -> transactionService.transfer(transferRequest, USER_ID))
                .isInstanceOf(InsufficientFundsException.class);
    }

    @Test
    void transferThrowsOnFrozenAccount() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(destAccount));
        when(transferExecutor.executeTransfer(any(), any(), any(), any(), any()))
                .thenThrow(new AccountFrozenException(1L));

        assertThatThrownBy(() -> transactionService.transfer(transferRequest, USER_ID))
                .isInstanceOf(AccountFrozenException.class);
    }

    @Test
    void transferThrowsOnMissingAccount() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(destAccount));
        when(transferExecutor.executeTransfer(any(), any(), any(), any(), any()))
                .thenThrow(new AccountNotFoundException(99L));

        assertThatThrownBy(() -> transactionService.transfer(transferRequest, USER_ID))
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void depositSuccessfully() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(sourceAccount));
        when(transferExecutor.executeDeposit(eq(1L), eq(BigDecimal.valueOf(500)),
                eq("Test deposit"), eq("idem-dep-1"))).thenReturn(mockTxResponse);

        TransactionResponse result = transactionService.deposit(depositRequest, USER_ID);

        assertThat(result.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(500));
        assertThat(result.getType()).isEqualTo(TransactionType.CREDIT);
    }

    @Test
    void depositThrowsWhenUserDoesNotOwnAccount() {
        Account otherUsersAccount = new Account(999L, "Other", "USD");
        otherUsersAccount.setId(1L);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(otherUsersAccount));

        assertThatThrownBy(() -> transactionService.deposit(depositRequest, USER_ID))
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void withdrawSuccessfully() {
        TransactionResponse withdrawResp = new TransactionResponse(1L, 1L, TransactionType.DEBIT,
                BigDecimal.valueOf(50), "Withdrawal", null, TransactionStatus.COMPLETED, BigDecimal.valueOf(950), null);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(sourceAccount));
        when(transferExecutor.executeWithdraw(eq(1L), eq(BigDecimal.valueOf(50)),
                eq("Test withdraw"), eq("idem-wd-1"))).thenReturn(withdrawResp);

        TransactionResponse result = transactionService.withdraw(withdrawRequest, USER_ID);

        assertThat(result.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(50));
        assertThat(result.getType()).isEqualTo(TransactionType.DEBIT);
    }

    @Test
    void withdrawThrowsOnInsufficientFunds() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(sourceAccount));
        when(transferExecutor.executeWithdraw(any(), any(), any(), any()))
                .thenThrow(new InsufficientFundsException(BigDecimal.valueOf(10), BigDecimal.valueOf(50)));

        assertThatThrownBy(() -> transactionService.withdraw(withdrawRequest, USER_ID))
                .isInstanceOf(InsufficientFundsException.class);
    }

    @Test
    void getByIdSuccessfully() {
        Transaction tx = new Transaction();
        tx.setId(1L);
        tx.setAccountId(1L);
        tx.setType(TransactionType.CREDIT);
        tx.setAmount(BigDecimal.valueOf(100));
        tx.setStatus(TransactionStatus.COMPLETED);
        tx.setBalanceAfter(BigDecimal.valueOf(1100));
        tx.setIdempotencyKey("key");

        when(transactionRepository.findById(1L)).thenReturn(Optional.of(tx));
        when(accountRepository.findById(1L)).thenReturn(Optional.of(sourceAccount));

        TransactionResponse result = transactionService.getById(1L, USER_ID);

        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void getByIdThrowsWhenNotFound() {
        when(transactionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.getById(99L, USER_ID))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getByIdThrowsWhenUserDoesNotOwnAccount() {
        Transaction tx = new Transaction();
        tx.setId(1L);
        tx.setAccountId(1L);
        tx.setType(TransactionType.CREDIT);
        tx.setAmount(BigDecimal.valueOf(100));
        tx.setStatus(TransactionStatus.COMPLETED);
        tx.setBalanceAfter(BigDecimal.valueOf(1100));
        tx.setIdempotencyKey("key");

        Account otherUsersAccount = new Account(999L, "Other", "USD");
        otherUsersAccount.setId(1L);

        when(transactionRepository.findById(1L)).thenReturn(Optional.of(tx));
        when(accountRepository.findById(1L)).thenReturn(Optional.of(otherUsersAccount));

        assertThatThrownBy(() -> transactionService.getById(1L, USER_ID))
                .isInstanceOf(AccountNotFoundException.class);
    }
}
