package org.example.balanceservcie.exception;

import lombok.Getter;

@Getter
public class BalanceException extends RuntimeException {
    private final String errorCode;

    public BalanceException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

}
