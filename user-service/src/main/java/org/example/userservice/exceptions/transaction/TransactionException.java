package org.example.userservice.exceptions.transaction;

import lombok.Getter;
import lombok.NonNull;

@Getter
public class TransactionException extends RuntimeException {

    private static final String DEFAULT_ERROR_CODE = "DEFAULT";

    private final String errorCode;

    public TransactionException(@NonNull String errorCode, @NonNull String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public TransactionException(@NonNull String message) {
        super(message);
        this.errorCode = DEFAULT_ERROR_CODE;
    }
}
