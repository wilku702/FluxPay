package com.payflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payflow.dto.AccountResponse;
import com.payflow.dto.CreateAccountRequest;
import com.payflow.dto.UpdateAccountStatusRequest;
import com.payflow.exception.AccountNotFoundException;
import com.payflow.model.AccountStatus;
import com.payflow.service.AccountService;
import com.payflow.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice test for {@link AccountController}.
 *
 * {@link TestSecurityConfig} provides a permissive {@code SecurityFilterChain} that
 * disables CSRF and permits all requests. Authentication is still supplied via
 * {@code .with(user("1"))} on each request so that Spring MVC can resolve the
 * {@code Authentication} parameter that the controller injects via
 * {@code Long.parseLong(authentication.getName())}.
 *
 * Only {@link AccountService} needs to be mocked; infrastructure beans are satisfied
 * by the test {@code application.yml} and {@link TestSecurityConfig}.
 */
@WebMvcTest(AccountController.class)
@Import(TestSecurityConfig.class)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AccountService accountService;

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

    private static final AccountResponse STUB_ACCOUNT = new AccountResponse(
            1L, USER_ID, "Checking", BigDecimal.valueOf(1000), "USD",
            AccountStatus.ACTIVE, LocalDateTime.now());

    // -------------------------------------------------------------------------
    // POST /api/accounts
    // -------------------------------------------------------------------------

    @Test
    void createAccountReturns201() throws Exception {
        when(accountService.create(eq(USER_ID), any(CreateAccountRequest.class))).thenReturn(STUB_ACCOUNT);

        CreateAccountRequest request = new CreateAccountRequest("Checking", "USD");

        mockMvc.perform(post("/api/accounts")
                        .with(user("1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.accountName").value("Checking"))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    // -------------------------------------------------------------------------
    // GET /api/accounts
    // -------------------------------------------------------------------------

    @Test
    void getAllAccountsReturns200() throws Exception {
        when(accountService.getByUserId(USER_ID)).thenReturn(List.of(STUB_ACCOUNT));

        mockMvc.perform(get("/api/accounts").with(user("1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].accountName").value("Checking"));
    }

    @Test
    void getAllAccountsReturnsEmptyListWhenNoneExist() throws Exception {
        when(accountService.getByUserId(USER_ID)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/accounts").with(user("1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    // -------------------------------------------------------------------------
    // GET /api/accounts/{id}
    // -------------------------------------------------------------------------

    @Test
    void getAccountByIdReturns200() throws Exception {
        when(accountService.getById(1L, USER_ID)).thenReturn(STUB_ACCOUNT);

        mockMvc.perform(get("/api/accounts/1").with(user("1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.accountName").value("Checking"))
                .andExpect(jsonPath("$.balance").value(1000));
    }

    @Test
    void getAccountReturns404WhenNotFound() throws Exception {
        when(accountService.getById(99L, USER_ID)).thenThrow(new AccountNotFoundException(99L));

        mockMvc.perform(get("/api/accounts/99").with(user("1")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Account not found: 99"));
    }

    // -------------------------------------------------------------------------
    // PATCH /api/accounts/{id}/status
    // -------------------------------------------------------------------------

    @Test
    void updateStatusReturns200() throws Exception {
        AccountResponse frozenAccount = new AccountResponse(
                1L, USER_ID, "Checking", BigDecimal.valueOf(1000), "USD",
                AccountStatus.FROZEN, LocalDateTime.now());

        when(accountService.updateStatus(eq(1L), eq(USER_ID), any(UpdateAccountStatusRequest.class)))
                .thenReturn(frozenAccount);

        UpdateAccountStatusRequest request = new UpdateAccountStatusRequest(AccountStatus.FROZEN);

        mockMvc.perform(patch("/api/accounts/1/status")
                        .with(user("1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("FROZEN"));
    }
}
