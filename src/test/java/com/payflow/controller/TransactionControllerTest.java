package com.payflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payflow.dto.DepositRequest;
import com.payflow.dto.TransactionResponse;
import com.payflow.dto.TransferRequest;
import com.payflow.dto.TransferResponse;
import com.payflow.dto.WithdrawRequest;
import com.payflow.model.TransactionStatus;
import com.payflow.model.TransactionType;
import com.payflow.service.TransactionService;
import com.payflow.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice test for {@link TransactionController}.
 *
 * {@link TestSecurityConfig} provides a permissive {@code SecurityFilterChain} that
 * disables CSRF and permits all requests. Authentication is supplied via
 * {@code .with(user("1"))} on each request so that Spring MVC resolves the
 * {@code Authentication} parameter — the principal name {@code "1"} satisfies
 * {@code Long.parseLong(authentication.getName())} == {@code 1L}.
 *
 * Only {@link TransactionService} needs to be mocked; infrastructure beans are
 * satisfied by the test {@code application.yml} and {@link TestSecurityConfig}.
 */
@WebMvcTest(TransactionController.class)
@Import(TestSecurityConfig.class)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TransactionService transactionService;

    /**
     * {@code JwtUtil} lives in the {@code util} package which is outside the
     * {@code @WebMvcTest} component-scan scope, so it is not auto-detected.
     * {@code JwtAuthenticationFilter} (in the {@code config} package) is
     * auto-detected and needs a {@code JwtUtil} bean to satisfy its constructor.
     * Providing a mock here satisfies that dependency without requiring real JWT
     * configuration. The filter itself runs but never sees a Bearer header in
     * these tests, so it simply passes the request through.
     */
    @MockBean
    private JwtUtil jwtUtil;

    private static final Long USER_ID = 1L;
    private static final UUID CORRELATION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static final TransactionResponse STUB_CREDIT_TX = new TransactionResponse(
            1L, 1L, TransactionType.CREDIT, BigDecimal.valueOf(500),
            "Deposit", null, TransactionStatus.COMPLETED, BigDecimal.valueOf(1500),
            LocalDateTime.now());

    private static final TransactionResponse STUB_DEBIT_TX = new TransactionResponse(
            2L, 1L, TransactionType.DEBIT, BigDecimal.valueOf(100),
            "Withdraw", null, TransactionStatus.COMPLETED, BigDecimal.valueOf(900),
            LocalDateTime.now());

    private static final TransferResponse STUB_TRANSFER_RESPONSE = new TransferResponse(
            CORRELATION_ID,
            new TransactionResponse(3L, 1L, TransactionType.DEBIT, BigDecimal.valueOf(200),
                    "Transfer", CORRELATION_ID, TransactionStatus.COMPLETED, BigDecimal.valueOf(800),
                    LocalDateTime.now()),
            new TransactionResponse(4L, 2L, TransactionType.CREDIT, BigDecimal.valueOf(200),
                    "Transfer", CORRELATION_ID, TransactionStatus.COMPLETED, BigDecimal.valueOf(1200),
                    LocalDateTime.now()));

    // -------------------------------------------------------------------------
    // POST /api/transactions/deposit
    // -------------------------------------------------------------------------

    @Test
    void depositReturns201() throws Exception {
        when(transactionService.deposit(any(DepositRequest.class), eq(USER_ID))).thenReturn(STUB_CREDIT_TX);

        DepositRequest request = new DepositRequest(1L, BigDecimal.valueOf(500), "Deposit", "idem-key-1");

        mockMvc.perform(post("/api/transactions/deposit")
                        .with(user("1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.type").value("CREDIT"))
                .andExpect(jsonPath("$.amount").value(500))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void depositReturns400WithInvalidRequest() throws Exception {
        // Empty body — required fields are null so Bean Validation fires
        mockMvc.perform(post("/api/transactions/deposit")
                        .with(user("1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------------------
    // POST /api/transactions/withdraw
    // -------------------------------------------------------------------------

    @Test
    void withdrawReturns201() throws Exception {
        when(transactionService.withdraw(any(WithdrawRequest.class), eq(USER_ID))).thenReturn(STUB_DEBIT_TX);

        WithdrawRequest request = new WithdrawRequest(1L, BigDecimal.valueOf(100), "Withdraw", "idem-key-2");

        mockMvc.perform(post("/api/transactions/withdraw")
                        .with(user("1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.type").value("DEBIT"))
                .andExpect(jsonPath("$.amount").value(100));
    }

    // -------------------------------------------------------------------------
    // POST /api/transactions/transfer
    // -------------------------------------------------------------------------

    @Test
    void transferReturns201() throws Exception {
        when(transactionService.transfer(any(TransferRequest.class), eq(USER_ID)))
                .thenReturn(STUB_TRANSFER_RESPONSE);

        TransferRequest request = new TransferRequest(1L, 2L, BigDecimal.valueOf(200), "Transfer", "idem-key-3");

        mockMvc.perform(post("/api/transactions/transfer")
                        .with(user("1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.correlationId").value(CORRELATION_ID.toString()))
                .andExpect(jsonPath("$.debit.type").value("DEBIT"))
                .andExpect(jsonPath("$.credit.type").value("CREDIT"))
                .andExpect(jsonPath("$.debit.amount").value(200));
    }

    // -------------------------------------------------------------------------
    // GET /api/transactions
    // -------------------------------------------------------------------------

    @Test
    void getTransactionsReturns200() throws Exception {
        PageImpl<TransactionResponse> page =
                new PageImpl<>(List.of(STUB_CREDIT_TX), PageRequest.of(0, 20, Sort.by("createdAt").descending()), 1);

        when(transactionService.getTransactions(
                eq(1L), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any(), eq(USER_ID)))
                .thenReturn(page);

        mockMvc.perform(get("/api/transactions")
                        .with(user("1"))
                        .param("accountId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getTransactionsClampsSizeTo100() throws Exception {
        PageImpl<TransactionResponse> page =
                new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 100, Sort.by("createdAt").descending()), 0);

        when(transactionService.getTransactions(
                eq(1L), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any(), eq(USER_ID)))
                .thenReturn(page);

        // Request size=999 — the controller must clamp it to MAX_PAGE_SIZE (100)
        mockMvc.perform(get("/api/transactions")
                        .with(user("1"))
                        .param("accountId", "1")
                        .param("size", "999"))
                .andExpect(status().isOk());

        // Verify the Pageable passed to the service has pageSize clamped to 100
        verify(transactionService).getTransactions(
                eq(1L), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                argThat(pageable -> pageable.getPageSize() == 100),
                eq(USER_ID));
    }

    @Test
    void getTransactionsDefaultsSortByToCreatedAt() throws Exception {
        PageImpl<TransactionResponse> page =
                new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 20, Sort.by("createdAt").descending()), 0);

        when(transactionService.getTransactions(
                eq(1L), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any(), eq(USER_ID)))
                .thenReturn(page);

        // "invalid" is not in ALLOWED_SORT_FIELDS — the controller replaces it with "createdAt"
        mockMvc.perform(get("/api/transactions")
                        .with(user("1"))
                        .param("accountId", "1")
                        .param("sortBy", "invalid"))
                .andExpect(status().isOk());

        verify(transactionService).getTransactions(
                eq(1L), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                argThat(pageable -> pageable.getSort().getOrderFor("createdAt") != null),
                eq(USER_ID));
    }

    // -------------------------------------------------------------------------
    // GET /api/transactions/{id}
    // -------------------------------------------------------------------------

    @Test
    void getTransactionByIdReturns200() throws Exception {
        when(transactionService.getById(1L, USER_ID)).thenReturn(STUB_CREDIT_TX);

        mockMvc.perform(get("/api/transactions/1").with(user("1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.type").value("CREDIT"))
                .andExpect(jsonPath("$.amount").value(500));
    }
}
