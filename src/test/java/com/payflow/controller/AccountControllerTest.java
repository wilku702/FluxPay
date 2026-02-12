package com.payflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payflow.dto.AccountResponse;
import com.payflow.dto.CreateAccountRequest;
import com.payflow.dto.UpdateAccountStatusRequest;
import com.payflow.exception.AccountNotFoundException;
import com.payflow.model.AccountStatus;
import com.payflow.service.AccountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ContextConfiguration;
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

/**
 * Slice test for {@link AccountController}.
 *
 * A minimal {@link TestSecurityConfig} replaces the production {@code SecurityConfig}
 * to keep all security filters running (so {@code @WithMockUser} can populate
 * the {@link org.springframework.security.core.context.SecurityContext}) while
 * permitting every request without requiring a JWT. This avoids the
 * {@code JwtAuthenticationFilter @MockBean} pitfall where the mock intercepts the
 * filter chain without forwarding it, resulting in empty response bodies.
 *
 * {@code @WithMockUser(username = "1")} satisfies
 * {@code Long.parseLong(authentication.getName())} in every controller method.
 */
@WebMvcTest(AccountController.class)
@Import(AccountControllerTest.TestSecurityConfig.class)
@WithMockUser(username = "1")
class AccountControllerTest {

    /**
     * Minimal security configuration for the test slice: permits all requests
     * and disables CSRF so no {@code csrf()} post-processor is needed on
     * mutating requests.
     */
    static class TestSecurityConfig {
        @Bean
        SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
            http.csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AccountService accountService;

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
