package com.payflow.controller;

import com.payflow.dto.AccountResponse;
import com.payflow.exception.AccountNotFoundException;
import com.payflow.model.AccountStatus;
import com.payflow.model.DailySummary;
import com.payflow.repository.DailySummaryRepository;
import com.payflow.service.AccountService;
import com.payflow.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DailySummaryController.class)
@Import(TestSecurityConfig.class)
class DailySummaryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DailySummaryRepository dailySummaryRepository;

    @MockBean
    private AccountService accountService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private com.payflow.service.RateLimitService rateLimitService;

    @MockBean
    private com.payflow.service.MetricsService metricsService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        org.mockito.Mockito.when(rateLimitService.isAllowed(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(new com.payflow.service.RateLimitService.RateLimitResult(true, 100, 99, 0));
    }

    private static final Long USER_ID = 1L;

    @Test
    void getSummariesReturns200() throws Exception {
        AccountResponse stubAccount = new AccountResponse(
                1L, USER_ID, "Checking", BigDecimal.valueOf(1000), "USD",
                AccountStatus.ACTIVE, LocalDateTime.now());
        when(accountService.getById(1L, USER_ID)).thenReturn(stubAccount);

        DailySummary summary = new DailySummary(1L, LocalDate.of(2024, 6, 15));
        summary.setId(1L);
        summary.setTotalCredits(BigDecimal.valueOf(500));
        summary.setTotalDebits(BigDecimal.valueOf(100));
        summary.setTransactionCount(3);
        summary.setClosingBalance(BigDecimal.valueOf(1400));

        when(dailySummaryRepository.findByAccountIdAndSummaryDateBetweenOrderBySummaryDateAsc(
                eq(1L), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(summary));

        mockMvc.perform(get("/api/accounts/1/summaries")
                        .with(user("1"))
                        .param("from", "2024-06-01")
                        .param("to", "2024-06-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].accountId").value(1))
                .andExpect(jsonPath("$[0].totalCredits").value(500))
                .andExpect(jsonPath("$[0].transactionCount").value(3));
    }

    @Test
    void getSummariesReturns404WhenAccountNotOwned() throws Exception {
        when(accountService.getById(99L, USER_ID)).thenThrow(new AccountNotFoundException(99L));

        mockMvc.perform(get("/api/accounts/99/summaries")
                        .with(user("1"))
                        .param("from", "2024-06-01")
                        .param("to", "2024-06-30"))
                .andExpect(status().isNotFound());
    }
}
