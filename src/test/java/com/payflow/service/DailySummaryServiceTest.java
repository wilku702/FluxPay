package com.payflow.service;

import com.payflow.dto.AccountResponse;
import com.payflow.exception.AccountNotFoundException;
import com.payflow.model.AccountStatus;
import com.payflow.model.DailySummary;
import com.payflow.repository.DailySummaryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DailySummaryServiceTest {

    @Mock
    private DailySummaryRepository dailySummaryRepository;
    @Mock
    private AccountService accountService;

    @InjectMocks
    private DailySummaryService dailySummaryService;

    @Test
    void getSummariesReturnsSummaries() {
        Long accountId = 1L;
        Long userId = 1L;
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 1, 31);

        AccountResponse accountResponse = new AccountResponse(accountId, userId, "Checking",
                BigDecimal.valueOf(1000), "USD", AccountStatus.ACTIVE, null);
        when(accountService.getById(accountId, userId)).thenReturn(accountResponse);

        DailySummary summary = new DailySummary(accountId, LocalDate.of(2026, 1, 15));
        summary.setTotalCredits(BigDecimal.valueOf(500));
        summary.setTotalDebits(BigDecimal.valueOf(200));
        summary.setTransactionCount(3);
        when(dailySummaryRepository.findByAccountIdAndSummaryDateBetweenOrderBySummaryDateAsc(accountId, from, to))
                .thenReturn(List.of(summary));

        List<DailySummary> result = dailySummaryService.getSummaries(accountId, from, to, userId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAccountId()).isEqualTo(accountId);
        assertThat(result.get(0).getTotalCredits()).isEqualByComparingTo(BigDecimal.valueOf(500));
        assertThat(result.get(0).getTransactionCount()).isEqualTo(3);
        verify(accountService).getById(accountId, userId);
        verify(dailySummaryRepository).findByAccountIdAndSummaryDateBetweenOrderBySummaryDateAsc(accountId, from, to);
    }

    @Test
    void getSummariesThrowsWhenUserDoesNotOwnAccount() {
        Long accountId = 1L;
        Long userId = 99L;
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 1, 31);

        when(accountService.getById(accountId, userId)).thenThrow(new AccountNotFoundException(accountId));

        assertThatThrownBy(() -> dailySummaryService.getSummaries(accountId, from, to, userId))
                .isInstanceOf(AccountNotFoundException.class);

        verify(accountService).getById(accountId, userId);
        verifyNoInteractions(dailySummaryRepository);
    }
}
