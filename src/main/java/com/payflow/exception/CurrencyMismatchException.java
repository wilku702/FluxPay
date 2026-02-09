package com.payflow.exception;

public class CurrencyMismatchException extends RuntimeException {
    public CurrencyMismatchException(String sourceCurrency, String destCurrency) {
        super("Currency mismatch: cannot transfer between " + sourceCurrency + " and " + destCurrency);
    }
}
