package com.payflow.dto;

import com.payflow.model.AccountStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateAccountStatusRequest(
        @NotNull(message = "Status is required") AccountStatus status
) {}
