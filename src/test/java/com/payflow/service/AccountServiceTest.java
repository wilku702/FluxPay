package com.payflow.service;

import com.payflow.dto.AccountResponse;
import com.payflow.dto.CreateAccountRequest;
import com.payflow.dto.UpdateAccountStatusRequest;
import com.payflow.exception.AccountNotFoundException;
import com.payflow.model.Account;
import com.payflow.model.AccountStatus;
import com.payflow.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AccountService accountService;

    private Account testAccount;

    @BeforeEach
    void setUp() {
        testAccount = new Account(1L, "Checking", "USD");
        testAccount.setId(1L);
        testAccount.setBalance(BigDecimal.valueOf(1000));
    }

    @Test
    void createAccountSuccessfully() {
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        AccountResponse response = accountService.create(1L, new CreateAccountRequest("Checking", "USD"));

        assertThat(response.getAccountName()).isEqualTo("Checking");
        assertThat(response.getCurrency()).isEqualTo("USD");
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void getByIdReturnsAccount() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));

        AccountResponse response = accountService.getById(1L, 1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(1000));
    }

    @Test
    void getByIdThrowsWhenNotFound() {
        when(accountRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.getById(99L, 1L))
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void getByIdThrowsWhenWrongUser() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));

        assertThatThrownBy(() -> accountService.getById(1L, 999L))
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void getByUserIdReturnsList() {
        when(accountRepository.findByUserId(1L)).thenReturn(List.of(testAccount));

        List<AccountResponse> result = accountService.getByUserId(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAccountName()).isEqualTo("Checking");
    }

    @Test
    void updateStatusSuccessfully() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        AccountResponse response = accountService.updateStatus(1L, 1L,
                new UpdateAccountStatusRequest(AccountStatus.FROZEN));

        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void updateStatusThrowsWhenWrongUser() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));

        assertThatThrownBy(() -> accountService.updateStatus(1L, 999L,
                new UpdateAccountStatusRequest(AccountStatus.FROZEN)))
                .isInstanceOf(AccountNotFoundException.class);
    }
}
