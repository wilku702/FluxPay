package com.payflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateAccountRequest(
        @NotBlank(message = "Account name is required")
        @Size(max = 50, message = "Account name must not exceed 50 characters") String accountName,
        @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO 4217 code") String currency
) {
    public CreateAccountRequest {
        if (currency == null) currency = "USD";
    }
}
