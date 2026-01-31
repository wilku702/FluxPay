package com.payflow.exception;

public class AccountFrozenException extends RuntimeException {
    public AccountFrozenException(Long id) {
        super("Account is frozen or closed: " + id);
    }
}
