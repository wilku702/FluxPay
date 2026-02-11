package com.payflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payflow.config.JwtAuthenticationFilter;
import com.payflow.dto.AccountResponse;
import com.payflow.dto.CreateAccountRequest;
import com.payflow.dto.UpdateAccountStatusRequest;
import com.payflow.exception.AccountNotFoundException;
import com.payflow.model.AccountStatus;
import com.payflow.service.AccountService;
import com.payflow.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountController.class)
@AutoConfigureMockMvc(addFilters = false)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AccountService accountService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private static final Long USER_ID = 1L;

    private static final AccountResponse STUB_ACCOUNT = new AccountResponse(
            1L, USER_ID, "Checking", BigDecimal.valueOf(1000), "USD",
            AccountStatus.ACTIVE, LocalDateTime.now());

    /**
     * The AccountController reads the authenticated user ID via
     * {@code Long.parseLong(authentication.getName())}. We inject a
     * UsernamePasswordAuthenticationToken whose principal name is "1" into the
     * SecurityContext before every test so that the controller can parse it
     * without Spring Security filters running.
     */
    @BeforeEach
    void setUpSecurityContext() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("1", null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // -------------------------------------------------------------------------
    // POST /api/accounts
    // -------------------------------------------------------------------------

    @Test
    void createAccountReturns201() throws Exception {
        when(accountService.create(eq(USER_ID), any(CreateAccountRequest.class))).thenReturn(STUB_ACCOUNT);

        CreateAccountRequest request = new CreateAccountRequest("Checking", "USD");

        mockMvc.perform(post("/api/accounts")
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

        mockMvc.perform(get("/api/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].accountName").value("Checking"));
    }

    @Test
    void getAllAccountsReturnsEmptyListWhenNoneExist() throws Exception {
        when(accountService.getByUserId(USER_ID)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/accounts"))
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

        mockMvc.perform(get("/api/accounts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.accountName").value("Checking"))
                .andExpect(jsonPath("$.balance").value(1000));
    }

    @Test
    void getAccountReturns404WhenNotFound() throws Exception {
        when(accountService.getById(99L, USER_ID)).thenThrow(new AccountNotFoundException(99L));

        mockMvc.perform(get("/api/accounts/99"))
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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("FROZEN"));
    }
}
