package com.payflow.exception;

import com.payflow.dto.ErrorResponse;
import com.payflow.dto.TransactionResponse;
import com.payflow.model.Transaction;
import com.payflow.model.TransactionStatus;
import com.payflow.model.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure unit tests for {@link GlobalExceptionHandler}.
 *
 * The handler is a plain Spring bean — no application context is needed.
 * We instantiate it directly, call each {@code @ExceptionHandler} method, and
 * assert the returned {@link ResponseEntity}.
 */
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    // -------------------------------------------------------------------------
    // AccountNotFoundException -> 404
    // -------------------------------------------------------------------------

    @Test
    void handleNotFoundReturns404() {
        AccountNotFoundException ex = new AccountNotFoundException(42L);

        ResponseEntity<ErrorResponse> response = handler.handleNotFound(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getMessage()).contains("42");
    }

    // -------------------------------------------------------------------------
    // AccountFrozenException -> 403
    // -------------------------------------------------------------------------

    @Test
    void handleFrozenReturns403() {
        AccountFrozenException ex = new AccountFrozenException(7L);

        ResponseEntity<ErrorResponse> response = handler.handleFrozen(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(403);
        assertThat(response.getBody().getMessage()).contains("7");
    }

    // -------------------------------------------------------------------------
    // InsufficientFundsException -> 409
    // -------------------------------------------------------------------------

    @Test
    void handleInsufficientFundsReturns409() {
        InsufficientFundsException ex =
                new InsufficientFundsException(BigDecimal.valueOf(50), BigDecimal.valueOf(200));

        ResponseEntity<ErrorResponse> response = handler.handleInsufficientFunds(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(409);
        assertThat(response.getBody().getMessage()).contains("50");
        assertThat(response.getBody().getMessage()).contains("200");
    }

    // -------------------------------------------------------------------------
    // DuplicateTransactionException -> 200 with TransactionResponse body
    // -------------------------------------------------------------------------

    @Test
    void handleDuplicateReturns200() {
        Transaction tx = new Transaction();
        tx.setId(10L);
        tx.setAccountId(1L);
        tx.setType(TransactionType.CREDIT);
        tx.setAmount(BigDecimal.valueOf(100));
        tx.setDescription("Original deposit");
        tx.setCorrelationId(UUID.randomUUID());
        tx.setIdempotencyKey("idem-key-abc");
        tx.setStatus(TransactionStatus.COMPLETED);
        tx.setBalanceAfter(BigDecimal.valueOf(1100));

        DuplicateTransactionException ex = new DuplicateTransactionException(tx);

        ResponseEntity<TransactionResponse> response = handler.handleDuplicate(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(10L);
        assertThat(response.getBody().getType()).isEqualTo(TransactionType.CREDIT);
        assertThat(response.getBody().getAmount()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(response.getBody().getStatus()).isEqualTo(TransactionStatus.COMPLETED);
    }

    // -------------------------------------------------------------------------
    // CurrencyMismatchException -> 400
    // -------------------------------------------------------------------------

    @Test
    void handleCurrencyMismatchReturns400() {
        CurrencyMismatchException ex = new CurrencyMismatchException("USD", "EUR");

        ResponseEntity<ErrorResponse> response = handler.handleCurrencyMismatch(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getMessage()).contains("USD");
        assertThat(response.getBody().getMessage()).contains("EUR");
    }

    // -------------------------------------------------------------------------
    // EmailAlreadyExistsException -> 409
    // -------------------------------------------------------------------------

    @Test
    void handleEmailExistsReturns409() {
        EmailAlreadyExistsException ex = new EmailAlreadyExistsException("dup@example.com");

        ResponseEntity<ErrorResponse> response = handler.handleEmailExists(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(409);
        assertThat(response.getBody().getMessage()).contains("dup@example.com");
    }

    // -------------------------------------------------------------------------
    // InvalidCredentialsException -> 401
    // -------------------------------------------------------------------------

    @Test
    void handleInvalidCredentialsReturns401() {
        InvalidCredentialsException ex = new InvalidCredentialsException();

        ResponseEntity<ErrorResponse> response = handler.handleInvalidCredentials(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(401);
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid email or password");
    }

    // -------------------------------------------------------------------------
    // ObjectOptimisticLockingFailureException -> 409
    // -------------------------------------------------------------------------

    @Test
    void handleOptimisticLockReturns409() {
        ObjectOptimisticLockingFailureException ex =
                new ObjectOptimisticLockingFailureException("com.payflow.model.Account", 5L);

        ResponseEntity<ErrorResponse> response = handler.handleOptimisticLock(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(409);
        assertThat(response.getBody().getMessage()).contains("Concurrent modification conflict");
    }

    // -------------------------------------------------------------------------
    // IllegalArgumentException -> 400
    // -------------------------------------------------------------------------

    @Test
    void handleIllegalArgumentReturns400() {
        IllegalArgumentException ex = new IllegalArgumentException("Source and destination accounts must be different");

        ResponseEntity<ErrorResponse> response = handler.handleIllegalArgument(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getMessage())
                .isEqualTo("Source and destination accounts must be different");
    }
}
