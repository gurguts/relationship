package org.example.userservice.exceptions.transaction;

import lombok.Getter;

@Getter
public class TransactionException extends RuntimeException {
    private final String errorCode;

    public TransactionException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public TransactionException(String message) {
        super(message);
        this.errorCode = "DEFAULT";
    }
}
