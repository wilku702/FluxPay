package com.payflow.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record TransferRequest(
        @NotNull(message = "Source account ID is required") Long sourceAccountId,
        @NotNull(message = "Destination account ID is required") Long destinationAccountId,
        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be positive")
        @Digits(integer = 15, fraction = 4, message = "Amount exceeds precision limits")
        BigDecimal amount,
        @Size(max = 255, message = "Description must not exceed 255 characters") String description,
        @NotBlank(message = "Idempotency key is required")
        @Size(max = 100, message = "Idempotency key must not exceed 100 characters") String idempotencyKey
) {}
