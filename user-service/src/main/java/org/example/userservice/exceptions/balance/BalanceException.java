package org.example.userservice.exceptions.balance;

import lombok.Getter;

@Getter
public class BalanceException extends RuntimeException {
    private final String errorCode;

    public BalanceException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public BalanceException(String message) {
        super(message);
        errorCode = "DEFAULT_BALANCE";
    }
}
