package org.example.transactionservice.exceptions;

import lombok.Getter;

@Getter
public class TransactionException extends RuntimeException{
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
