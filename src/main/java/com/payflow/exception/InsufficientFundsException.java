package com.payflow.exception;

import java.math.BigDecimal;

public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(BigDecimal currentBalance, BigDecimal requestedAmount) {
        super("Insufficient funds. Current balance: " + currentBalance + ", requested: " + requestedAmount);
    }
}
